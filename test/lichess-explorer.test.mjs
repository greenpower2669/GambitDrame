import test from 'node:test';
import assert from 'node:assert/strict';

import {
  normalizeExplorerSnapshot,
  mergeLichessIntoBranch,
  normalizeUciSequence,
} from '../src/lichess-explorer.mjs';

test('normalizes whitespace UCI into one position key', () => {
  assert.deepEqual(
    normalizeUciSequence('d2d4 d7d5 c2c4 e7e6'),
    ['d2d4', 'd7d5', 'c2c4', 'e7e6']
  );
});

test('computes move frequency from position aggregate', () => {
  const snapshot = normalizeExplorerSnapshot({
    white: 50,
    draws: 20,
    black: 30,
    moves: [
      { uci: 'a1c1', san: 'Rc1', white: 30, draws: 10, black: 10, averageRating: 2100, game: null, opening: null },
    ],
    topGames: [],
    recentGames: [],
    opening: { eco: 'D60', name: "Queen's Gambit Declined" },
  }, 'd2d4 d7d5 c2c4 e7e6');

  assert.equal(snapshot.total, 100);
  assert.equal(snapshot.moves[0].games, 50);
  assert.equal(snapshot.moves[0].frequency, 0.5);
});

test('refresh replaces Lichess aggregate instead of incrementing it', () => {
  const first = normalizeExplorerSnapshot({
    white: 50,
    draws: 20,
    black: 30,
    moves: [{ uci: 'a1c1', san: 'Rc1', white: 30, draws: 10, black: 10, averageRating: 2100, game: null, opening: null }],
    topGames: [], recentGames: [], opening: null,
  }, ['d2d4']);

  const second = normalizeExplorerSnapshot({
    white: 55,
    draws: 21,
    black: 34,
    moves: [{ uci: 'a1c1', san: 'Rc1', white: 33, draws: 11, black: 11, averageRating: 2110, game: null, opening: null }],
    topGames: [], recentGames: [], opening: null,
  }, ['d2d4']);

  const once = mergeLichessIntoBranch({ moves: [] }, first);
  const twice = mergeLichessIntoBranch(once, second);

  assert.equal(once.lichess.totalGames, 100);
  assert.equal(twice.lichess.totalGames, 110);
  assert.equal(twice.moves[0].lichess.games, 55);
});
