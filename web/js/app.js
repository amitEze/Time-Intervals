// App logic: screens, button handling and running the timer.
import { buildStages, totalSec, formatMmSs } from './stages.js';
import { TimerEngine } from './engine.js';
import * as store from './storage.js';
import * as sound from './sound.js';
import { listView, editView, runView, updateRun, settingsView } from './views.js';

const app = document.getElementById('app');
const state = {
  screen: 'list',
  workouts: store.loadWorkouts(),
  settings: store.loadSettings(),
  draft: null, // copy of the workout being edited
  run: null, // { workout, engine, lastIndex }
};
let tickTimer = null;
let lastSchedule = 0;
let wakeLock = null;

// ---- Navigation (the phone's back button works too) ----

function show(screen) {
  if ((screen === 'edit' && !state.draft) || (screen === 'run' && !state.run)) screen = 'list';
  state.screen = screen;
  if (screen === 'list') app.innerHTML = listView(state.workouts, state.run);
  if (screen === 'edit') app.innerHTML = editView(state.draft);
  if (screen === 'settings') app.innerHTML = settingsView(state.settings);
  if (screen === 'run') {
    app.innerHTML = runView(state.run.workout.name);
    updateRun(state.run.engine, performance.now());
  }
  window.scrollTo(0, 0);
}

function go(screen) {
  if (screen === 'list' && history.state?.screen) return history.back();
  if (screen !== 'list') history.pushState({ screen }, '');
  show(screen);
}

window.addEventListener('popstate', () => show(history.state?.screen ?? 'list'));

// ---- Buttons ----

const find = (id) => state.workouts.find((w) => w.id === id);
const saveAll = () => store.saveWorkouts(state.workouts);
const redrawEditor = () => { const y = window.scrollY; show('edit'); window.scrollTo(0, y); };

const actions = {
  new: () => { state.draft = store.newWorkout(); go('edit'); },
  edit: (d) => { state.draft = structuredClone(find(d.id)); go('edit'); },
  copy: (d) => {
    const w = find(d.id);
    const c = { ...structuredClone(w), id: store.newId(), name: `${w.name} (copy)` };
    c.blocks = c.blocks.map((b) => ({ ...b, id: store.newId() }));
    state.workouts.splice(state.workouts.indexOf(w) + 1, 0, c);
    saveAll(); show('list');
  },
  delete: (d) => {
    const w = find(d.id);
    if (!confirm(`Delete “${w.name}”?`)) return;
    state.workouts = state.workouts.filter((x) => x.id !== w.id);
    saveAll(); show('list');
  },
  settings: () => go('settings'),
  back: () => go('list'),
  'open-run': () => go('run'),
  start: (d) => startRun(find(d.id)),

  // Editor
  cancel: () => { state.draft = null; go('list'); },
  save: () => {
    const w = state.draft;
    w.name = w.name.trim() || 'Workout';
    const i = state.workouts.findIndex((x) => x.id === w.id);
    if (i >= 0) state.workouts[i] = w; else state.workouts.push(w);
    saveAll(); state.draft = null; go('list');
  },
  toggle: (d, el) => { state.draft[d.key] = el.checked; redrawEditor(); },
  'add-block': () => {
    const last = state.draft.blocks.at(-1);
    state.draft.blocks.push(store.newBlock(last ? { workSec: last.workSec, restSec: last.restSec, rounds: last.rounds } : {}));
    redrawEditor();
  },
  rounds: (d) => {
    const b = state.draft.blocks[+d.i];
    b.rounds = Math.min(99, Math.max(1, b.rounds + +d.d));
    redrawEditor();
  },
  move: (d) => {
    const bs = state.draft.blocks, i = +d.i, j = i + +d.d;
    if (j < 0 || j >= bs.length) return;
    [bs[i], bs[j]] = [bs[j], bs[i]];
    redrawEditor();
  },
  remove: (d) => { state.draft.blocks.splice(+d.i, 1); redrawEditor(); },

  // Settings
  setting: (d, el) => {
    state.settings[d.key] = el.checked;
    store.saveSettings(state.settings);
    if (state.run) reschedule();
  },

  // Run screen
  pause: () => {
    const e = state.run.engine, now = performance.now();
    if (e.isPaused) e.resume(now); else e.pause(now);
    reschedule(); updateRun(e, now);
  },
  skip: () => jump((e, now) => e.skip(now)),
  prev: () => jump((e, now) => e.previous(now)),
  stop: () => { endRun(); go('list'); },
  close: () => { endRun(); go('list'); },
};

app.addEventListener('click', (ev) => {
  const el = ev.target.closest('[data-action]');
  if (!el || el.disabled || el.type === 'checkbox') return;
  actions[el.dataset.action]?.(el.dataset, el);
});
app.addEventListener('change', (ev) => {
  const el = ev.target;
  if (el.type === 'checkbox' && el.dataset.action) actions[el.dataset.action](el.dataset, el);
});

// Editor text/number fields: update the draft as you type.
app.addEventListener('input', (ev) => {
  const el = ev.target, field = el.dataset.field;
  if (!field || !state.draft) return;
  if (field === 'name') { state.draft.name = el.value; return; }
  const path = field.split('.'); // e.g. "blocks.0.workSec" or "warmupSec"
  const key = path.pop();
  const obj = path.reduce((o, p) => o[p], state.draft);
  const n = Math.min(Number(el.max), Math.max(0, parseInt(el.value, 10) || 0));
  obj[key] = el.dataset.part === 'min' ? n * 60 + (obj[key] % 60) : Math.floor(obj[key] / 60) * 60 + n;
  document.getElementById('total').textContent = formatMmSs(totalSec(state.draft) * 1000);
});
app.addEventListener('focusout', (ev) => {
  const el = ev.target;
  if (el.dataset.part) el.value = Math.min(Number(el.max), Math.max(0, parseInt(el.value, 10) || 0));
});

// ---- Timer ----

function startRun(workout) {
  const stages = buildStages(workout);
  if (!stages.length) return alert('This workout has no stages.');
  sound.unlockAudio(); // must happen during the tap
  const engine = new TimerEngine(stages);
  const now = performance.now();
  engine.start(now);
  state.run = { workout, engine, lastIndex: 0 };
  stageChanged();
  reschedule();
  keepScreenOn();
  clearInterval(tickTimer);
  tickTimer = setInterval(tick, 200);
  go('run');
}

function tick() {
  const run = state.run;
  if (!run) return;
  const now = performance.now();
  run.engine.update(now);
  if (run.engine.index !== run.lastIndex) {
    run.lastIndex = run.engine.index;
    if (state.settings.vibration) sound.vibrate(run.engine.isFinished ? 600 : 250);
  }
  if (run.engine.isFinished) finishRun();
  else if (now - lastSchedule > 10000) reschedule(); // top up the beep queue
  if (state.screen === 'run') updateRun(run.engine, now);
}

function stageChanged() {
  if (state.settings.sound) sound.longBeepNow();
  if (state.settings.vibration) sound.vibrate();
}

function jump(fn) {
  const run = state.run, now = performance.now();
  fn(run.engine, now);
  run.lastIndex = run.engine.index; // user action: no beep/vibration
  reschedule();
  if (run.engine.isFinished) finishRun();
  updateRun(run.engine, now);
}

// Schedules the next 15 minutes of beeps on the audio clock.
function reschedule() {
  lastSchedule = performance.now();
  if (!state.run || !state.settings.sound) return sound.cancelAll();
  sound.schedule(state.run.engine.upcomingBeeps(lastSchedule, 15 * 60 * 1000));
}

function finishRun() {
  clearInterval(tickTimer);
  releaseScreen();
}

function endRun() {
  clearInterval(tickTimer);
  sound.cancelAll();
  releaseScreen();
  state.run = null;
}

// ---- Keep the screen on (Screen Wake Lock API) ----

async function keepScreenOn() {
  try { wakeLock = await navigator.wakeLock?.request('screen'); } catch { /* not supported or denied */ }
}
function releaseScreen() {
  wakeLock?.release().catch(() => {});
  wakeLock = null;
}
document.addEventListener('visibilitychange', () => {
  // The wake lock is dropped when the page is hidden; take it again on return.
  if (document.visibilityState === 'visible' && state.run && !state.run.engine.isFinished) {
    keepScreenOn();
    tick();
    reschedule();
  }
});

// ---- Start ----

history.replaceState(null, ''); // start fresh on reload
show('list');
if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
