const DEFAULT_FILTERS = Object.freeze({
  variant: 'standard',
  speeds: ['blitz', 'rapid', 'classical'],
  ratings: [1600, 1800, 2000, 2200, 2500],
  moves: 12,
  topGames: 4,
  recentGames: 4,
});

function asCsv(value) {
  return Array.isArray(value) ? value.join(',') : value;
}

function clampInt(value, min, max, fallback) {
  const n = Number.parseInt(value, 10);
  if (!Number.isFinite(n)) return fallback;
  return Math.min(max, Math.max(min, n));
}

export function normalizeUciSequence(play) {
  if (Array.isArray(play)) return play.map(String).map(s => s.trim()).filter(Boolean);
  if (!play) return [];
  return String(play)
    .split(/[\s,]+/)
    .map(s => s.trim())
    .filter(Boolean);
}

export function makePositionKey(play) {
  return normalizeUciSequence(play).join(' ');
}

export async function fetchLichessPosition(play, options = {}) {
  const sequence = normalizeUciSequence(play);
  const filters = { ...DEFAULT_FILTERS, ...(options.filters || {}) };
  const endpoint = options.endpoint || '/api/lichess-explorer';

  const params = new URLSearchParams({
    variant: filters.variant || 'standard',
    play: sequence.join(','),
    moves: String(clampInt(filters.moves, 1, 50, 12)),
    topGames: String(clampInt(filters.topGames, 0, 4, 4)),
    recentGames: String(clampInt(filters.recentGames, 0, 4, 4)),
  });

  if (filters.speeds?.length) params.set('speeds', asCsv(filters.speeds));
  if (filters.ratings?.length) params.set('ratings', asCsv(filters.ratings));
  if (filters.since) params.set('since', filters.since);
  if (filters.until) params.set('until', filters.until);

  const response = await fetch(`${endpoint}?${params.toString()}`, {
    headers: { Accept: 'application/json' },
    signal: options.signal,
  });

  if (!response.ok) {
    const retryAfter = response.headers.get('retry-after');
    const body = await response.text().catch(() => '');
    const error = new Error(`Lichess Explorer HTTP ${response.status}${body ? `: ${body.slice(0, 180)}` : ''}`);
    error.status = response.status;
    error.retryAfter = retryAfter;
    throw error;
  }

  const raw = await response.json();
  return normalizeExplorerSnapshot(raw, sequence);
}

export function normalizeExplorerSnapshot(raw, play = []) {
  const white = Number(raw?.white || 0);
  const draws = Number(raw?.draws || 0);
  const black = Number(raw?.black || 0);
  const total = white + draws + black;

  const moves = Array.isArray(raw?.moves)
    ? raw.moves.map(move => {
        const mw = Number(move.white || 0);
        const md = Number(move.draws || 0);
        const mb = Number(move.black || 0);
        const games = mw + md + mb;
        return {
          uci: move.uci,
          san: move.san,
          games,
          frequency: total > 0 ? games / total : 0,
          white: mw,
          draws: md,
          black: mb,
          averageRating: move.averageRating ?? null,
          opening: move.opening ?? null,
          exampleGame: move.game ?? null,
        };
      })
    : [];

  moves.sort((a, b) => b.games - a.games);

  return {
    source: 'lichess-opening-explorer',
    positionKey: makePositionKey(play),
    opening: raw?.opening ?? null,
    total,
    white,
    draws,
    black,
    moves,
    topGames: dedupeGames(raw?.topGames || []),
    recentGames: dedupeGames(raw?.recentGames || []),
    fetchedAt: new Date().toISOString(),
  };
}

export function dedupeGames(games) {
  const byId = new Map();
  for (const game of games || []) {
    if (!game?.id) continue;
    byId.set(game.id, game);
  }
  return [...byId.values()];
}

export function classifyMove(move, thresholds = {}) {
  const mainFrequency = thresholds.mainFrequency ?? 0.12;
  const candidateFrequency = thresholds.candidateFrequency ?? 0.03;
  const minMainGames = thresholds.minMainGames ?? 100;
  const minCandidateGames = thresholds.minCandidateGames ?? 30;

  if (move.games >= minMainGames && move.frequency >= mainFrequency) return 'main';
  if (move.games >= minCandidateGames && move.frequency >= candidateFrequency) return 'candidate';
  return 'rare';
}

/**
 * Merge a fresh Lichess snapshot into an existing GambitDrame branch.
 * Curated pedagogical fields are preserved. Lichess aggregate counts are replaced,
 * never added, because each Explorer response is already a complete snapshot.
 */
export function mergeLichessIntoBranch(branch = {}, snapshot, options = {}) {
  if (!snapshot) return branch;

  const previousMoves = new Map(
    (branch.moves || []).map(move => [move.uci, move])
  );

  const mergedMoves = snapshot.moves.map(lm => {
    const previous = previousMoves.get(lm.uci) || {};
    return {
      ...previous,
      uci: lm.uci,
      san: previous.san || lm.san,
      theoryLevel: previous.theoryLevel || classifyMove(lm, options.thresholds),
      lichess: {
        games: lm.games,
        frequency: lm.frequency,
        white: lm.white,
        draws: lm.draws,
        black: lm.black,
        averageRating: lm.averageRating,
        opening: lm.opening,
        fetchedAt: snapshot.fetchedAt,
      },
    };
  });

  // Preserve hand-curated moves that are currently outside the selected Lichess filters.
  for (const oldMove of branch.moves || []) {
    if (!snapshot.moves.some(m => m.uci === oldMove.uci)) mergedMoves.push(oldMove);
  }

  return {
    ...branch,
    positionKey: branch.positionKey || snapshot.positionKey,
    opening: branch.opening || snapshot.opening,
    lichess: {
      totalGames: snapshot.total,
      white: snapshot.white,
      draws: snapshot.draws,
      black: snapshot.black,
      topGames: snapshot.topGames,
      recentGames: snapshot.recentGames,
      fetchedAt: snapshot.fetchedAt,
      source: snapshot.source,
    },
    moves: mergedMoves,
  };
}

export function learningCandidates(branch, options = {}) {
  const max = options.max ?? 6;
  const includeRare = options.includeRare ?? false;
  return (branch?.moves || [])
    .filter(move => move?.lichess?.games > 0)
    .filter(move => includeRare || move.theoryLevel !== 'rare')
    .sort((a, b) => (b.lichess?.games || 0) - (a.lichess?.games || 0))
    .slice(0, max);
}
