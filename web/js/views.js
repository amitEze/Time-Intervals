// HTML for each screen. Buttons use data-action="..." (handled in app.js);
// inputs use data-field="..." to say which workout value they edit.
import { totalSec, formatMmSs, progressText } from './stages.js';

const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const t = (sec) => `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`;
// e.g. "5× (Fight 3:00, Rest 1:00) · total 29:45"
const summary = (w) =>
  w.blocks.map((b) => `${b.sets}× (` + b.stages.map((s) => `${s.name || (s.rest ? 'Rest' : 'Work')} ${t(s.sec)}`).join(', ') + ')').join(', ') +
  ` · total ${formatMmSs(totalSec(w) * 1000)}`;

export function listView(workouts, run) {
  const banner = run
    ? `<button class="banner stage-${run.engine.current?.type ?? 'DONE'}" data-action="open-run">
         ▶ ${esc(run.workout.name)} ${run.engine.isFinished ? 'finished' : 'is running'}. Tap to open</button>`
    : '';
  const cards = workouts.map((w) => `
    <div class="card">
      <h2>${esc(w.name)}</h2>
      <p class="muted">${summary(w)}</p>
      <button class="primary big" data-action="start" data-id="${w.id}">▶ Start</button>
      <div class="row">
        <button data-action="edit" data-id="${w.id}">Edit</button>
        <button data-action="copy" data-id="${w.id}">Copy</button>
        <button data-action="delete" data-id="${w.id}">Delete</button>
      </div>
    </div>`).join('');
  return `
    <header><h1>Interval Timer</h1><button class="flat" data-action="settings">Settings</button></header>
    <main>${banner}${cards || '<p>No workouts yet. Tap “New workout”.</p>'}</main>
    <button class="fab" data-action="new">+ New workout</button>`;
}

function duration(field, sec) {
  return `<div class="duration">
      <label>min<input type="number" inputmode="numeric" min="0" max="99" data-field="${field}" data-part="min" value="${Math.floor(sec / 60)}"></label>
      <span>:</span>
      <label>sec<input type="number" inputmode="numeric" min="0" max="59" data-field="${field}" data-part="sec" value="${sec % 60}"></label>
    </div>`;
}

function optional(title, key, enabled, sec) {
  return `<div class="card">
      <label class="switch-row">${title}<input type="checkbox" data-action="toggle" data-key="${key}" ${enabled ? 'checked' : ''}></label>
      ${enabled ? duration(key === 'warmupEnabled' ? 'warmupSec' : 'cooldownSec', sec) : ''}
    </div>`;
}

// One stage inside a block: name, Work/Rest switch (sets the color), duration.
function stageCard(st, i, j, count) {
  return `
    <div class="stage-card ${st.rest ? 'is-rest' : 'is-work'}">
      <div class="stage-top">
        <label class="name">Stage ${j + 1} name<input type="text" data-field="blocks.${i}.stages.${j}.name" value="${esc(st.name)}"></label>
        <button class="kind" data-action="stage-kind" data-i="${i}" data-j="${j}">${st.rest ? 'Rest' : 'Work'}</button>
      </div>
      ${duration(`blocks.${i}.stages.${j}.sec`, st.sec)}
      <div class="row">
        <button data-action="stage-move" data-i="${i}" data-j="${j}" data-d="-1" ${j === 0 ? 'disabled' : ''}>↑</button>
        <button data-action="stage-move" data-i="${i}" data-j="${j}" data-d="1" ${j === count - 1 ? 'disabled' : ''}>↓</button>
        <button data-action="stage-remove" data-i="${i}" data-j="${j}" ${count === 1 ? 'disabled' : ''}>Remove</button>
      </div>
    </div>`;
}

export function editView(w) {
  const blocks = w.blocks.map((b, i) => `
    <div class="card">
      <h3>Block ${i + 1}</h3>
      <label class="name">Block name (optional)<input type="text" data-field="blocks.${i}.name" value="${esc(b.name)}"></label>
      <p>Sets</p>
      <div class="stepper">
        <button data-action="sets" data-i="${i}" data-d="-1">−</button>
        <span>${b.sets}</span>
        <button data-action="sets" data-i="${i}" data-d="1">+</button>
      </div>
      <p>Stages (run in order, every set)</p>
      ${b.stages.map((st, j) => stageCard(st, i, j, b.stages.length)).join('')}
      <button data-action="add-stage" data-i="${i}">+ Add stage</button>
      <div class="row">
        <button data-action="move" data-i="${i}" data-d="-1" ${i === 0 ? 'disabled' : ''}>↑ Up</button>
        <button data-action="move" data-i="${i}" data-d="1" ${i === w.blocks.length - 1 ? 'disabled' : ''}>↓ Down</button>
        <button data-action="remove" data-i="${i}" ${w.blocks.length === 1 ? 'disabled' : ''}>Remove block</button>
      </div>
    </div>`).join('');
  return `
    <header><button class="flat" data-action="cancel">Cancel</button><h1>Edit workout</h1><button class="flat" data-action="save">Save</button></header>
    <main>
      <label class="name">Name<input type="text" data-field="name" value="${esc(w.name)}"></label>
      <p class="total">Total: <span id="total">${formatMmSs(totalSec(w) * 1000)}</span></p>
      ${optional('Start (warm-up)', 'warmupEnabled', w.warmupEnabled, w.warmupSec)}
      ${blocks}
      <button class="big" data-action="add-block">+ Add block</button>
      ${optional('End (cool-down)', 'cooldownEnabled', w.cooldownEnabled, w.cooldownSec)}
      <button class="primary big" data-action="save">Save</button>
    </main>`;
}

export function runView(name) {
  return `
    <div class="run" id="run">
      <p class="run-name">${esc(name)}</p>
      <p class="run-stage" id="r-stage"></p>
      <p class="run-round" id="r-round"></p>
      <p class="run-time" id="r-time"></p>
      <p class="run-total" id="r-total"></p>
      <p class="run-next" id="r-next"></p>
      <div class="run-controls" id="r-controls">
        <div class="row">
          <button data-action="prev">⏮ Prev</button>
          <button class="wide" data-action="pause" id="r-pause">⏸ Pause</button>
          <button data-action="skip">Skip ⏭</button>
        </div>
        <div class="row">
          <button class="ghost" data-action="back">Back</button>
          <button class="ghost" data-action="stop">■ Stop</button>
        </div>
      </div>
      <button class="big hidden" id="r-close" data-action="close">Close</button>
    </div>`;
}

// Updates the run screen text (called several times a second).
export function updateRun(engine, now) {
  const s = engine.current;
  const $ = (id) => document.getElementById(id);
  $('run').className = `run stage-${s?.type ?? 'DONE'}`;
  if (!s) {
    $('r-stage').textContent = 'Done!';
    ['r-round', 'r-time', 'r-total', 'r-next'].forEach((id) => ($(id).textContent = ''));
    $('r-controls').classList.add('hidden');
    $('r-close').classList.remove('hidden');
    return;
  }
  $('r-stage').textContent = s.name + (engine.isPaused ? ' (paused)' : '');
  $('r-round').textContent = progressText(s);
  $('r-time').textContent = formatMmSs(engine.stageRemainingMs(now));
  $('r-total').textContent = `Total left ${formatMmSs(engine.totalRemainingMs(now))}`;
  const next = engine.stages[engine.index + 1];
  $('r-next').textContent = next ? `Next: ${next.name} ${formatMmSs(next.durationSec * 1000)}` : '';
  $('r-pause').textContent = engine.isPaused ? '▶ Resume' : '⏸ Pause';
}

export function settingsView(s) {
  return `
    <header><button class="flat" data-action="back">Back</button><h1>Settings</h1><span></span></header>
    <main>
      <label class="switch-row">Sound (countdown beeps)<input type="checkbox" data-action="setting" data-key="sound" ${s.sound ? 'checked' : ''}></label>
      <label class="switch-row">Vibrate on stage change<input type="checkbox" data-action="setting" data-key="vibration" ${s.vibration ? 'checked' : ''}></label>
      <p class="muted">Keep this page open with the screen on while training. Workouts are saved in this browser only.</p>
    </main>`;
}
