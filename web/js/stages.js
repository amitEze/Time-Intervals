// Turns a workout into the flat list of stages the timer runs.
// Same rules as the Android app (StageBuilder.kt):
// - Start/End only when enabled and longer than 0 s.
// - Each block adds Work (and Rest) for every round.
// - The rest after the very last round of the last block is skipped.
// - Stages of 0 s are skipped.

export const STAGE_LABEL = { START: 'Start', WORK: 'Work', REST: 'Rest', END: 'End' };

export function buildStages(w) {
  const stages = [];
  if (w.warmupEnabled && w.warmupSec > 0) stages.push(stage('START', w.warmupSec));

  const blocks = w.blocks.filter((b) => b.rounds > 0);
  blocks.forEach((b, bi) => {
    for (let r = 1; r <= b.rounds; r++) {
      const lastOfAll = bi === blocks.length - 1 && r === b.rounds;
      const info = { block: bi + 1, blockCount: blocks.length, round: r, roundCount: b.rounds };
      if (b.workSec > 0) stages.push(stage('WORK', b.workSec, info));
      if (!lastOfAll && b.restSec > 0) stages.push(stage('REST', b.restSec, info));
    }
  });

  if (w.cooldownEnabled && w.cooldownSec > 0) stages.push(stage('END', w.cooldownSec));
  return stages;
}

function stage(type, durationSec, info = { block: 0, blockCount: 0, round: 0, roundCount: 0 }) {
  return { type, durationSec, ...info };
}

export function totalSec(w) {
  return buildStages(w).reduce((sum, s) => sum + s.durationSec, 0);
}

// Milliseconds -> "MM:SS", rounded up so 00:00 only shows at the very end.
export function formatMmSs(ms) {
  const t = Math.ceil(Math.max(0, ms) / 1000);
  return String(Math.floor(t / 60)).padStart(2, '0') + ':' + String(t % 60).padStart(2, '0');
}
