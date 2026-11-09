// Turns a workout into the flat list of stages the timer runs.
// Same rules as the Android app (StageBuilder.kt):
// - Start/End only when enabled and longer than 0 s.
// - Each block runs all its stages, in order, once per set.
// - Rest stages at the very end of the last set of the last block are skipped.
// - Stages of 0 s are skipped.

export const STAGE_LABEL = { START: 'Start', WORK: 'Work', REST: 'Rest', END: 'End' };

export function buildStages(w) {
  const stages = [];
  if (w.warmupEnabled && w.warmupSec > 0) stages.push(plain('START', w.warmupSec));

  const blocks = w.blocks.filter((b) => b.sets > 0 && b.stages.some((s) => s.sec > 0));
  blocks.forEach((b, bi) => {
    const steps = b.stages.filter((s) => s.sec > 0);
    for (let set = 1; set <= b.sets; set++) {
      let run = steps;
      if (bi === blocks.length - 1 && set === b.sets) {
        // In the very last set, drop trailing rest stages.
        let end = steps.length;
        while (end > 0 && steps[end - 1].rest) end--;
        run = steps.slice(0, end);
      }
      for (const st of run) {
        stages.push({
          type: st.rest ? 'REST' : 'WORK',
          durationSec: st.sec,
          name: st.name.trim() || (st.rest ? 'Rest' : 'Work'),
          block: bi + 1, blockCount: blocks.length, blockName: b.name ?? '',
          set, setCount: b.sets,
        });
      }
    }
  });

  if (w.cooldownEnabled && w.cooldownSec > 0) stages.push(plain('END', w.cooldownSec));
  return stages;
}

function plain(type, durationSec) {
  return { type, durationSec, name: STAGE_LABEL[type], block: 0, blockCount: 0, blockName: '', set: 0, setCount: 0 };
}

// e.g. "Set 3/5 · Boxing 1/2", or "" for Start/End.
export function progressText(s) {
  if (!s.set) return '';
  let block = '';
  if (s.blockCount > 1 || s.blockName.trim()) {
    block = ' · ' + (s.blockName.trim() || 'Block') + (s.blockCount > 1 ? ` ${s.block}/${s.blockCount}` : '');
  }
  return `Set ${s.set}/${s.setCount}${block}`;
}

export function totalSec(w) {
  return buildStages(w).reduce((sum, s) => sum + s.durationSec, 0);
}

// Milliseconds -> "MM:SS", rounded up so 00:00 only shows at the very end.
export function formatMmSs(ms) {
  const t = Math.ceil(Math.max(0, ms) / 1000);
  return String(Math.floor(t / 60)).padStart(2, '0') + ':' + String(t % 60).padStart(2, '0');
}
