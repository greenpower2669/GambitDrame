import {
  fetchLichessPosition,
  makePositionKey,
  mergeLichessIntoBranch,
  normalizeUciSequence,
} from './lichess-explorer.mjs';
import {
  annotateLearningLevels,
  getLearningProfile,
  selectMovesForLevel,
} from './learning-policy.mjs';

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function retryDelayMs(error) {
  const raw = error?.retryAfter;
  if (raw) {
    const seconds = Number(raw);
    if (Number.isFinite(seconds)) return Math.max(60_000, seconds * 1000);
    const date = Date.parse(raw);
    if (Number.isFinite(date)) return Math.max(60_000, date - Date.now());
  }
  return 60_000;
}

function stableFilterKey(filters = {}) {
  const normalized = {};
  for (const key of Object.keys(filters).sort()) {
    const value = filters[key];
    normalized[key] = Array.isArray(value) ? [...value].sort() : value;
  }
  return JSON.stringify(normalized);
}

export class LichessStudyClient {
  constructor(options = {}) {
    this.fetchPosition = options.fetchPosition || fetchLichessPosition;
    this.ttlMs = options.ttlMs ?? 15 * 60_000;
    this.maxEntries = options.maxEntries ?? 500;
    this.cache = new Map();
    this.inflight = new Map();
    this.tail = Promise.resolve();
    this.cooldownUntil = 0;
  }

  cacheKey(play, options = {}) {
    return `${makePositionKey(play)}|${stableFilterKey(options.filters || {})}`;
  }

  clear() {
    this.cache.clear();
  }

  prune() {
    const now = Date.now();
    for (const [key, entry] of this.cache) {
      if (entry.expiresAt <= now) this.cache.delete(key);
    }
    while (this.cache.size > this.maxEntries) {
      const firstKey = this.cache.keys().next().value;
      if (firstKey === undefined) break;
      this.cache.delete(firstKey);
    }
  }

  async waitForCooldown() {
    const waitMs = this.cooldownUntil - Date.now();
    if (waitMs > 0) await sleep(waitMs);
  }

  async getPosition(play, options = {}) {
    const key = this.cacheKey(play, options);
    const now = Date.now();
    const cached = this.cache.get(key);
    if (!options.force && cached && cached.expiresAt > now) {
      return { ...cached.snapshot, cache: 'hit' };
    }

    if (this.inflight.has(key)) return this.inflight.get(key);

    const task = async () => {
      await this.waitForCooldown();
      try {
        const snapshot = await this.fetchPosition(play, options);
        this.cache.delete(key);
        this.cache.set(key, {
          snapshot,
          expiresAt: Date.now() + this.ttlMs,
        });
        this.prune();
        return { ...snapshot, cache: 'miss' };
      } catch (error) {
        if (error?.status === 429) {
          // Lichess asks API clients to pause for a full minute after a 429.
          this.cooldownUntil = Math.max(this.cooldownUntil, Date.now() + retryDelayMs(error));
        }
        throw error;
      }
    };

    // Lichess recommends one API request at a time. Every cache miss is therefore
    // serialized, even when several UI components ask for positions simultaneously.
    const promise = this.tail.then(task, task);
    this.tail = promise.catch(() => undefined);
    this.inflight.set(key, promise);

    try {
      return await promise;
    } finally {
      this.inflight.delete(key);
    }
  }
}

function initialTree(existingTree, rootPlay, level) {
  return {
    version: 1,
    source: 'lichess-opening-explorer',
    rootPositionKey: makePositionKey(rootPlay),
    level,
    generatedAt: existingTree?.generatedAt || null,
    updatedAt: null,
    positions: { ...(existingTree?.positions || {}) },
  };
}

/**
 * Increment/refresh a GambitDrame study tree from Lichess.
 *
 * Storage is position-based instead of deeply nested. This makes refreshes cheap:
 * a known position is replaced with the new aggregate snapshot while pedagogical
 * fields and hand-curated moves remain intact.
 */
export async function syncStudyTree(rootPlay, options = {}) {
  const level = options.level || 'advanced';
  const profile = getLearningProfile(level, options.profile || {});
  const maxDepth = options.maxDepth ?? (profile.key === 'advanced' ? 4 : profile.key === 'intermediate' ? 3 : 2);
  const maxPositions = options.maxPositions ?? (profile.key === 'advanced' ? 80 : profile.key === 'intermediate' ? 35 : 15);
  const client = options.client || new LichessStudyClient(options.clientOptions);
  const rootSequence = normalizeUciSequence(rootPlay);
  const tree = initialTree(options.existingTree, rootSequence, profile.key);

  const queue = [{ play: rootSequence, depth: 0 }];
  const queued = new Set([makePositionKey(rootSequence)]);
  const visited = new Set();

  while (queue.length && visited.size < maxPositions) {
    const current = queue.shift();
    const key = makePositionKey(current.play);
    if (visited.has(key)) continue;
    visited.add(key);

    const previous = tree.positions[key] || { positionKey: key, moves: [] };
    const snapshot = await client.getPosition(current.play, {
      endpoint: options.endpoint,
      filters: options.filters,
      signal: options.signal,
      force: options.force,
    });

    let branch = mergeLichessIntoBranch(previous, snapshot, {
      thresholds: options.thresholds,
    });
    branch = annotateLearningLevels(branch);

    const taughtMoves = selectMovesForLevel(branch, profile.key, profile);
    branch.study = {
      ...(branch.study || {}),
      depth: current.depth,
      taughtMoves: taughtMoves.map(move => move.uci),
      level: profile.key,
      refreshedAt: snapshot.fetchedAt,
    };

    tree.positions[key] = branch;

    if (current.depth >= maxDepth) continue;

    for (const move of taughtMoves) {
      if (!move?.uci) continue;
      const childPlay = [...current.play, move.uci];
      const childKey = makePositionKey(childPlay);
      move.childPositionKey = childKey;

      if (!queued.has(childKey) && visited.size + queue.length < maxPositions) {
        queued.add(childKey);
        queue.push({ play: childPlay, depth: current.depth + 1 });
      }
    }
  }

  tree.updatedAt = new Date().toISOString();
  tree.stats = {
    refreshedPositions: visited.size,
    storedPositions: Object.keys(tree.positions).length,
    maxDepth,
    maxPositions,
  };

  return tree;
}

export function getStudyPosition(tree, play) {
  return tree?.positions?.[makePositionKey(play)] || null;
}

export function getTaughtContinuations(tree, play) {
  const position = getStudyPosition(tree, play);
  if (!position) return [];
  const taught = new Set(position?.study?.taughtMoves || []);
  return (position.moves || []).filter(move => taught.has(move.uci));
}
