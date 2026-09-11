import test from 'node:test';
import assert from 'node:assert/strict';

import {
  LichessStudyClient,
  getTaughtContinuations,
  syncStudyTree,
} from '../src/lichess-study-sync.mjs';

function snapshotFor(play) {
  const depth = Array.isArray(play) ? play.length : String(play || '').split(/\s+/).filter(Boolean).length;
  return {
    source: 'lichess-opening-explorer',
    positionKey: Array.isArray(play) ? play.join(' ') : String(play || ''),
    opening: null,
    total: 1000,
    white: 450,
    draws: 250,
    black: 300,
    moves: depth >= 2 ? [] : [
      {
        uci: depth === 0 ? 'd2d4' : 'd7d5',
        san: depth === 0 ? 'd4' : 'd5',
        games: 800,
        frequency: 0.8,
        white: 360,
        draws: 200,
        black: 240,
        averageRating: 1900,
        opening: null,
        exampleGame: null,
      },
    ],
    topGames: [],
    recentGames: [],
    fetchedAt: '2026-09-09T20:00:00.000Z',
  };
}

test('cache prevents duplicate fetches for the same position', async () => {
  let calls = 0;
  const client = new LichessStudyClient({
    ttlMs: 60_000,
    fetchPosition: async play => {
      calls += 1;
      return snapshotFor(play);
    },
  });

  await client.getPosition(['d2d4']);
  await client.getPosition(['d2d4']);
  assert.equal(calls, 1);
});

test('concurrent requests are serialized', async () => {
  let active = 0;
  let maxActive = 0;
  const client = new LichessStudyClient({
    fetchPosition: async play => {
      active += 1;
      maxActive = Math.max(maxActive, active);
      await new Promise(resolve => setTimeout(resolve, 5));
      active -= 1;
      return snapshotFor(play);
    },
  });

  await Promise.all([
    client.getPosition(['d2d4']),
    client.getPosition(['e2e4']),
    client.getPosition(['c2c4']),
  ]);

  assert.equal(maxActive, 1);
});

test('study sync expands taught continuations into child positions', async () => {
  const client = new LichessStudyClient({
    fetchPosition: async play => snapshotFor(play),
  });

  const tree = await syncStudyTree([], {
    level: 'advanced',
    maxDepth: 2,
    maxPositions: 10,
    client,
  });

  assert.equal(tree.stats.refreshedPositions, 3);
  assert.ok(tree.positions['']);
  assert.ok(tree.positions['d2d4']);
  assert.ok(tree.positions['d2d4 d7d5']);
  assert.deepEqual(getTaughtContinuations(tree, []).map(move => move.uci), ['d2d4']);
});

test('existing curated move survives a Lichess refresh', async () => {
  const existingTree = {
    positions: {
      'd2d4': {
        positionKey: 'd2d4',
        moves: [
          { uci: 'a1c1', san: 'Rc1', pedagogy: 'Tour sur la colonne c' },
        ],
      },
    },
  };

  const client = new LichessStudyClient({
    fetchPosition: async play => ({ ...snapshotFor(play), moves: [] }),
  });

  const tree = await syncStudyTree(['d2d4'], {
    existingTree,
    level: 'advanced',
    maxDepth: 0,
    client,
  });

  assert.equal(tree.positions['d2d4'].moves[0].uci, 'a1c1');
  assert.equal(tree.positions['d2d4'].moves[0].pedagogy, 'Tour sur la colonne c');
});
