const LEVELS = Object.freeze({
  beginner: Object.freeze({
    key: 'beginner',
    label: 'Débutant',
    maxMoves: 2,
    minFrequency: 0.10,
    minGames: 200,
    includeRare: false,
    keepCurated: true,
  }),
  intermediate: Object.freeze({
    key: 'intermediate',
    label: 'Intermédiaire',
    maxMoves: 4,
    minFrequency: 0.035,
    minGames: 80,
    includeRare: false,
    keepCurated: true,
  }),
  advanced: Object.freeze({
    key: 'advanced',
    label: 'Avancé',
    maxMoves: 7,
    minFrequency: 0.01,
    minGames: 20,
    includeRare: true,
    keepCurated: true,
  }),
});

const LEVEL_ALIASES = Object.freeze({
  debutant: 'beginner',
  débutant: 'beginner',
  beginner: 'beginner',
  novice: 'beginner',
  intermediaire: 'intermediate',
  intermédiaire: 'intermediate',
  intermediate: 'intermediate',
  moyen: 'intermediate',
  avance: 'advanced',
  avancé: 'advanced',
  advanced: 'advanced',
});

export function normalizeLearningLevel(level = 'intermediate') {
  const raw = String(level || '').trim().toLowerCase();
  return LEVEL_ALIASES[raw] || 'intermediate';
}

export function getLearningProfile(level = 'intermediate', overrides = {}) {
  const key = normalizeLearningLevel(level);
  return { ...LEVELS[key], ...overrides, key };
}

function isCurated(move) {
  return Boolean(
    move?.curated ||
    move?.pedagogy ||
    move?.explanation ||
    move?.idea ||
    move?.theoretical === true ||
    move?.source === 'curated'
  );
}

function scoreMove(move) {
  const games = Number(move?.lichess?.games || 0);
  const frequency = Number(move?.lichess?.frequency || 0);
  const curatedBonus = isCurated(move) ? 1_000_000_000 : 0;
  return curatedBonus + games + frequency * 100_000;
}

/**
 * Select the continuations GambitDrame should actively teach for a level.
 * Hand-curated theory is preserved even when it is rare in the selected Lichess pool.
 */
export function selectMovesForLevel(branch, level = 'intermediate', overrides = {}) {
  const profile = getLearningProfile(level, overrides);
  const moves = Array.isArray(branch?.moves) ? branch.moves : [];

  const selected = moves.filter(move => {
    if (profile.keepCurated && isCurated(move)) return true;

    const games = Number(move?.lichess?.games || 0);
    const frequency = Number(move?.lichess?.frequency || 0);
    if (games < profile.minGames || frequency < profile.minFrequency) return false;
    if (!profile.includeRare && move?.theoryLevel === 'rare') return false;
    return true;
  });

  selected.sort((a, b) => scoreMove(b) - scoreMove(a));

  // Curated moves are never lost because of maxMoves. The cap applies to
  // statistically discovered moves only.
  const curated = selected.filter(isCurated);
  const discovered = selected.filter(move => !isCurated(move));
  const discoveredSlots = Math.max(0, profile.maxMoves - curated.length);

  return [...curated, ...discovered.slice(0, discoveredSlots)];
}

export function annotateLearningLevels(branch) {
  if (!branch || !Array.isArray(branch.moves)) return branch;

  const sets = {
    beginner: new Set(selectMovesForLevel(branch, 'beginner').map(move => move.uci)),
    intermediate: new Set(selectMovesForLevel(branch, 'intermediate').map(move => move.uci)),
    advanced: new Set(selectMovesForLevel(branch, 'advanced').map(move => move.uci)),
  };

  return {
    ...branch,
    moves: branch.moves.map(move => ({
      ...move,
      learningLevels: {
        beginner: sets.beginner.has(move.uci),
        intermediate: sets.intermediate.has(move.uci),
        advanced: sets.advanced.has(move.uci),
      },
    })),
  };
}

export const LEARNING_LEVELS = LEVELS;
