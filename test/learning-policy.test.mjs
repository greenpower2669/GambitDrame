import test from 'node:test';
import assert from 'node:assert/strict';

import {
  getLearningProfile,
  normalizeLearningLevel,
  selectMovesForLevel,
} from '../src/learning-policy.mjs';

const branch = {
  moves: [
    { uci: 'a1c1', pedagogy: 'Coup théorique maison', theoryLevel: 'rare', lichess: { games: 5, frequency: 0.001 } },
    { uci: 'd1c2', theoryLevel: 'main', lichess: { games: 5000, frequency: 0.30 } },
    { uci: 'f1d3', theoryLevel: 'main', lichess: { games: 3000, frequency: 0.18 } },
    { uci: 'c4d5', theoryLevel: 'candidate', lichess: { games: 700, frequency: 0.06 } },
    { uci: 'h2h3', theoryLevel: 'rare', lichess: { games: 60, frequency: 0.012 } },
  ],
};

test('accepts French level names', () => {
  assert.equal(normalizeLearningLevel('Avancé'), 'advanced');
  assert.equal(normalizeLearningLevel('Débutant'), 'beginner');
  assert.equal(getLearningProfile('Intermédiaire').key, 'intermediate');
});

test('beginner keeps curated theory and only strongest discovered move', () => {
  const selected = selectMovesForLevel(branch, 'beginner');
  assert.deepEqual(selected.map(move => move.uci), ['a1c1', 'd1c2']);
});

test('advanced admits thin but meaningful branches', () => {
  const selected = selectMovesForLevel(branch, 'advanced');
  assert.deepEqual(selected.map(move => move.uci), ['a1c1', 'd1c2', 'f1d3', 'c4d5', 'h2h3']);
});
