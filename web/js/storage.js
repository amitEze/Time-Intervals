// Saves workouts and settings in the browser (localStorage) on this device.
// Same workout format as the Android app.

const WORKOUTS = 'it.workouts';
const SETTINGS = 'it.settings';

export const newId = () => Math.random().toString(36).slice(2) + Date.now().toString(36);

export const newBlock = (from = {}) => ({ workSec: 30, restSec: 15, rounds: 8, ...from, id: newId() });

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

export const loadWorkouts = () => read(WORKOUTS, null) ?? examples();
export const saveWorkouts = (list) => write(WORKOUTS, list);

export const loadSettings = () => ({ sound: true, vibration: true, ...read(SETTINGS, {}) });
export const saveSettings = (s) => write(SETTINGS, s);

function examples() {
  return [
    {
      ...newWorkout(), name: 'Boxing rounds + Tabata',
      warmupEnabled: true, warmupSec: 120,
      blocks: [newBlock({ workSec: 180, restSec: 60, rounds: 5 }), newBlock({ workSec: 30, restSec: 15, rounds: 8 })],
      cooldownEnabled: true, cooldownSec: 120,
    },
    { ...newWorkout(), name: 'Tabata', warmupEnabled: true, warmupSec: 10, blocks: [newBlock({ workSec: 20, restSec: 10, rounds: 8 })] },
  ];
}
