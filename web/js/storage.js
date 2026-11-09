// Saves workouts and settings in the browser (localStorage) on this device.
// Same workout format as the Android app.

const WORKOUTS = 'it.workouts';
const SETTINGS = 'it.settings';

export const newId = () => Math.random().toString(36).slice(2) + Date.now().toString(36);

export const newStage = (name = 'Work', sec = 30, rest = false) => ({ id: newId(), name, sec, rest });

export const newBlock = () => ({ id: newId(), name: '', sets: 8, stages: [newStage('Work', 30), newStage('Rest', 15, true)] });

// Copy of a block (or workout's block) with fresh ids.
export const copyBlock = (b) => ({ ...structuredClone(b), id: newId(), stages: b.stages.map((s) => ({ ...s, id: newId() })) });

// Old saved blocks had workSec/restSec/rounds; turn them into stages.
function upgrade(w) {
  w.blocks = w.blocks.map((b) => (b.stages ? b : {
    id: b.id ?? newId(), name: '', sets: b.rounds ?? 1,
    stages: [newStage('Work', b.workSec ?? 30), newStage('Rest', b.restSec ?? 15, true)],
  }));
  return w;
}

export const newWorkout = () => ({
  id: newId(), name: 'New workout',
  warmupEnabled: false, warmupSec: 60,
  blocks: [newBlock()],
  cooldownEnabled: false, cooldownSec: 60,
});

function read(key, fallback) {
  try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : fallback; } catch { return fallback; }
}
function write(key, value) {
  try { localStorage.setItem(key, JSON.stringify(value)); } catch { /* storage full or blocked */ }
}

export const loadWorkouts = () => (read(WORKOUTS, null) ?? examples()).map(upgrade);
export const saveWorkouts = (list) => write(WORKOUTS, list);

export const loadSettings = () => ({ sound: true, vibration: true, ...read(SETTINGS, {}) });
export const saveSettings = (s) => write(SETTINGS, s);

function examples() {
  const block = (name, sets, stages) => ({ id: newId(), name, sets, stages });
  return [
    {
      ...newWorkout(), name: 'Boxing rounds + Tabata',
      warmupEnabled: true, warmupSec: 120,
      blocks: [
        block('Boxing', 5, [newStage('Fight', 180), newStage('Rest', 60, true)]),
        block('Tabata', 8, [newStage('Burpees', 30), newStage('Rest', 15, true)]),
      ],
      cooldownEnabled: true, cooldownSec: 120,
    },
    {
      ...newWorkout(), name: 'Circuit', warmupEnabled: true, warmupSec: 10,
      blocks: [block('Circuit', 3, [newStage('Squats', 40), newStage('Push-ups', 40), newStage('Plank', 40), newStage('Rest', 60, true)])],
    },
  ];
}
