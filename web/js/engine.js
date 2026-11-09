// Pure timer logic (same as TimerEngine.kt in the Android app).
// Times are milliseconds from a monotonic clock (performance.now()).
// Remaining time is always computed from "now", never by adding up ticks,
// so it cannot drift even if the browser delays timers.

export class TimerEngine {
  constructor(stages) {
    this.stages = stages;
    this.index = 0; // current stage; equals stages.length when finished
    this.stageStart = 0; // clock time the current stage (virtually) started
    this.pausedAt = null; // clock time when paused, or null
  }

  get isPaused() { return this.pausedAt !== null; }
  get isFinished() { return this.index >= this.stages.length; }
  get current() { return this.stages[this.index] ?? null; }

  start(now) { this.index = 0; this.stageStart = now; this.pausedAt = null; }

  pause(now) { if (this.pausedAt === null && !this.isFinished) this.pausedAt = now; }

  resume(now) {
    if (this.pausedAt === null) return;
    this.stageStart += now - this.pausedAt; // shift by the time spent paused
    this.pausedAt = null;
  }

  // Moves past every stage whose time is up. Returns how many ended.
  update(now) {
    if (this.isPaused) return 0;
    let ended = 0;
    while (!this.isFinished && now - this.stageStart >= this.stages[this.index].durationSec * 1000) {
      this.stageStart += this.stages[this.index].durationSec * 1000; // exact, no drift
      this.index++;
      ended++;
    }
    return ended;
  }

  skip(now) { if (!this.isFinished) this.jumpTo(this.index + 1, now); }

  // Restarts the current stage if more than 3 s of it ran, else goes back one.
  previous(now) {
    if (this.isFinished) return this.jumpTo(this.stages.length - 1, now);
    const back = this.elapsedInStage(now) > 3000 || this.index === 0 ? this.index : this.index - 1;
    this.jumpTo(back, now);
  }

  jumpTo(target, now) {
    this.index = Math.min(Math.max(target, 0), this.stages.length);
    this.stageStart = now;
    if (this.pausedAt !== null) this.pausedAt = now; // stay paused at the stage start
  }

  elapsedInStage(now) { return (this.pausedAt ?? now) - this.stageStart; }

  stageRemainingMs(now) {
    const s = this.current;
    if (!s) return 0;
    return Math.min(Math.max(s.durationSec * 1000 - this.elapsedInStage(now), 0), s.durationSec * 1000);
  }

  totalRemainingMs(now) {
    if (this.isFinished) return 0;
    const later = this.stages.slice(this.index + 1).reduce((t, s) => t + s.durationSec * 1000, 0);
    return this.stageRemainingMs(now) + later;
  }

  // Upcoming sounds, as clock times: short beeps at 5..1 s before each
  // stage ends, and a long tone when each stage ends. Used to schedule audio
  // ahead of time so beeps stay on time even if the page is throttled.
  upcomingBeeps(now, horizonMs) {
    if (this.isPaused || this.isFinished) return [];
    const out = [];
    let start = this.stageStart;
    for (let i = this.index; i < this.stages.length; i++) {
      const dur = this.stages[i].durationSec;
      const end = start + dur * 1000;
      if (start > now + horizonMs) break;
      for (let k = Math.min(5, dur - 1); k >= 1; k--) {
        const t = end - k * 1000;
        if (t > now) out.push({ at: t, long: false });
      }
      if (end > now) out.push({ at: end, long: true });
      start = end;
    }
    return out.filter((b) => b.at <= now + horizonMs);
  }
}
