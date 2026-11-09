// Run with: node --test web/test/*.test.js   (CI runs this before every deploy)
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildStages, totalSec, formatMmSs } from '../js/stages.js';
import { TimerEngine } from '../js/engine.js';

const twoBlocks = {
  warmupEnabled: true, warmupSec: 60,
  blocks: [{ workSec: 180, restSec: 60, rounds: 5 }, { workSec: 30, restSec: 15, rounds: 8 }],
  cooldownEnabled: true, cooldownSec: 120,
};

test('builds stages in order and keeps rest between blocks', () => {
  const s = buildStages(twoBlocks);
  assert.equal(s.length, 1 + 10 + 15 + 1);
  assert.equal(s[0].type, 'START');
  assert.deepEqual(s[10], { type: 'REST', durationSec: 60, block: 1, blockCount: 2, round: 5, roundCount: 5 });
  assert.equal(s.at(-1).type, 'END');
});

test('skips the rest after the last round of the last block', () => {
  const s = buildStages(twoBlocks);
  assert.deepEqual(s.at(-2), { type: 'WORK', durationSec: 30, block: 2, blockCount: 2, round: 8, roundCount: 8 });
});

test('disabled or zero-length stages are left out', () => {
  const w = { warmupEnabled: false, warmupSec: 60, blocks: [{ workSec: 20, restSec: 0, rounds: 3 }], cooldownEnabled: true, cooldownSec: 0 };
  assert.deepEqual(buildStages(w).map((x) => x.type), ['WORK', 'WORK', 'WORK']);
});

test('total and formatting', () => {
  assert.equal(totalSec(twoBlocks), 60 + 900 + 300 + 240 + 105 + 120);
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
