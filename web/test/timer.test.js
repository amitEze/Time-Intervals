// Run with: node --test web/test/*.test.js   (CI runs this before every deploy)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildStages, totalSec, formatMmSs, progressText } from '../js/stages.js';
import { TimerEngine } from '../js/engine.js';

const work = (name, sec) => ({ name, sec, rest: false });
const rest = (sec) => ({ name: 'Rest', sec, rest: true });

const twoBlocks = {
  warmupEnabled: true, warmupSec: 60,
  blocks: [
    { name: 'Boxing', sets: 5, stages: [work('Fight', 180), rest(60)] },
    { name: 'Circuit', sets: 2, stages: [work('Squats', 30), work('Push-ups', 20), rest(15)] },
  ],
  cooldownEnabled: true, cooldownSec: 120,
};

test('builds stages in order and keeps rest between blocks', () => {
  const s = buildStages(twoBlocks);
  assert.equal(s.length, 1 + 10 + 5 + 1);
  assert.equal(s[0].type, 'START');
  assert.deepEqual(s[1], { type: 'WORK', durationSec: 180, name: 'Fight', block: 1, blockCount: 2, blockName: 'Boxing', set: 1, setCount: 5 });
  assert.deepEqual(s[10], { type: 'REST', durationSec: 60, name: 'Rest', block: 1, blockCount: 2, blockName: 'Boxing', set: 5, setCount: 5 });
  assert.deepEqual(s.slice(11, 16).map((x) => x.name), ['Squats', 'Push-ups', 'Rest', 'Squats', 'Push-ups']);
  assert.equal(s.at(-1).type, 'END');
});

test('only trailing rests of the very last set are skipped', () => {
  const w = { blocks: [{ name: '', sets: 2, stages: [rest(5), work('A', 10), rest(5), rest(3)] }] };
  assert.deepEqual(buildStages(w).map((x) => x.name + x.durationSec), ['Rest5', 'A10', 'Rest5', 'Rest3', 'Rest5', 'A10']);
});

test('disabled, empty or zero-length stages are left out; blank names get a default', () => {
  const w = {
    warmupEnabled: false, warmupSec: 60, cooldownEnabled: true, cooldownSec: 0,
    blocks: [
      { name: '', sets: 3, stages: [work(' ', 20), rest(0)] },
      { name: '', sets: 4, stages: [work('Nothing', 0)] },
    ],
  };
  const s = buildStages(w);
  assert.deepEqual(s.map((x) => x.name), ['Work', 'Work', 'Work']);
  assert.equal(s[0].blockCount, 1);
});

test('progress text', () => {
  const s = buildStages(twoBlocks);
  assert.equal(progressText(s[0]), '');
  assert.equal(progressText(s[1]), 'Set 1/5 · Boxing 1/2');
  const single = buildStages({ blocks: [{ name: '', sets: 3, stages: [work('A', 5)] }] });
  assert.equal(progressText(single[0]), 'Set 1/3');
});

test('total and formatting', () => {
  assert.equal(totalSec(twoBlocks), 60 + 1200 + 65 + 50 + 120);
  assert.equal(formatMmSs(180000), '03:00');
  assert.equal(formatMmSs(4001), '00:05');
  assert.equal(formatMmSs(1), '00:01');
  assert.equal(formatMmSs(0), '00:00');
});

const stages = [
  { type: 'WORK', durationSec: 10 }, { type: 'REST', durationSec: 5 }, { type: 'WORK', durationSec: 10 },
];
const started = (at = 0) => { const e = new TimerEngine(stages); e.start(at); return e; };

test('counts down from the clock and catches up after a long gap', () => {
  const e = started(1000);
  assert.equal(e.stageRemainingMs(4500), 6500);
  assert.equal(e.update(18000), 2);
  assert.equal(e.index, 2);
  assert.equal(e.stageRemainingMs(18000), 8000);
});

test('finishes exactly at the end', () => {
  const e = started();
  e.update(24999); assert.equal(e.isFinished, false);
  e.update(25000); assert.equal(e.isFinished, true);
});

test('pause freezes time', () => {
  const e = started();
  e.pause(3000);
  assert.equal(e.stageRemainingMs(60000), 7000);
  assert.equal(e.update(60000), 0);
  e.resume(60000);
  assert.equal(e.stageRemainingMs(65000), 2000);
});

test('skip and previous', () => {
  const e = started();
  e.skip(2000); assert.equal(e.index, 1);
  e.previous(3000); assert.equal(e.index, 0); // <3 s in: go back
  e.previous(8000); assert.equal(e.index, 0); // >3 s in: restart
  assert.equal(e.stageRemainingMs(8000), 10000);
  e.skip(0); e.skip(0); e.skip(0);
  assert.equal(e.isFinished, true);
});

test('upcoming beeps: 5..1 before each end, long tone at each end', () => {
  const e = started();
  const b = e.upcomingBeeps(0, 16000);
  // Work 10 s: beeps at 5..9 s, long at 10. Rest 5 s: beeps at 11..14 (only 4, not on its first second), long at 15.
  assert.deepEqual(b.map((x) => x.at), [5000, 6000, 7000, 8000, 9000, 10000, 11000, 12000, 13000, 14000, 15000]);
  assert.deepEqual(b.filter((x) => x.long).map((x) => x.at), [10000, 15000]);
  e.pause(1000);
  assert.deepEqual(e.upcomingBeeps(1000, 16000), []);
});
