// Beeps generated with the Web Audio API (no audio files).
// Beeps are scheduled ahead on the audio clock, so they stay on time even
// when the browser slows down the page's timers.

let ctx = null;
let scheduled = []; // { osc, at } not yet played

// Tweak sounds here: frequency in Hz, length in seconds.
const SHORT = { freq: 880, len: 0.15 };
const LONG = { freq: 1320, len: 0.6 };

// Must be called from a tap (browsers only allow audio after a user gesture).
export function unlockAudio() {
  if (!ctx) ctx = new (window.AudioContext || window.webkitAudioContext)();
  if (ctx.state === 'suspended') ctx.resume();
}

// Plays a tone at clock time `atMs` (performance.now() time base).
function tone({ freq, len }, atMs) {
  if (!ctx) return;
  const when = Math.max(ctx.currentTime, ctx.currentTime + (atMs - performance.now()) / 1000);
  const osc = ctx.createOscillator();
  const gain = ctx.createGain();
  osc.frequency.value = freq;
  // Short fade in/out so it doesn't click.
  gain.gain.setValueAtTime(0, when);
  gain.gain.linearRampToValueAtTime(0.6, when + 0.01);
  gain.gain.setValueAtTime(0.6, when + len - 0.02);
  gain.gain.linearRampToValueAtTime(0, when + len);
  osc.connect(gain).connect(ctx.destination);
  osc.start(when);
  osc.stop(when + len);
  scheduled.push({ osc, when });
  osc.onended = () => { scheduled = scheduled.filter((s) => s.osc !== osc); };
}

export const longBeepNow = () => tone(LONG, performance.now());

// Replaces all future beeps with the given list [{ at, long }].
export function schedule(beeps) {
  cancelAll();
  beeps.forEach((b) => tone(b.long ? LONG : SHORT, b.at));
}

// Stops beeps that haven't started yet (keeps one that is already playing).
export function cancelAll() {
  if (!ctx) return;
  const now = ctx.currentTime;
  scheduled.filter((s) => s.when > now + 0.01).forEach((s) => { try { s.osc.stop(); } catch { /* already stopped */ } });
  scheduled = scheduled.filter((s) => s.when <= now + 0.01);
}

export function vibrate(ms = 250) {
  if (navigator.vibrate) navigator.vibrate(ms);
}
