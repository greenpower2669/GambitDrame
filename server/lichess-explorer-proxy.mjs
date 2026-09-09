const EXPLORER_URL = 'https://explorer.lichess.org/lichess';

const ALLOWED = new Set([
  'variant',
  'fen',
  'play',
  'speeds',
  'ratings',
  'since',
  'until',
  'moves',
  'topGames',
  'recentGames',
  'history',
]);

let upstreamTail = Promise.resolve();
let cooldownUntil = 0;

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function copyAllowedParams(source) {
  const target = new URLSearchParams();
  for (const [key, value] of source.entries()) {
    if (ALLOWED.has(key) && value !== '') target.append(key, value);
  }
  return target;
}

function configuredOrigins() {
  return String(process.env.GAMBITDRAME_ALLOWED_ORIGIN || '')
    .split(',')
    .map(value => value.trim())
    .filter(Boolean);
}

function applyCors(req, res) {
  const requestOrigin = req.headers?.origin;
  if (!requestOrigin) return true; // same-origin/non-browser request: no CORS header needed

  const allowedOrigins = configuredOrigins();
  if (!allowedOrigins.includes(requestOrigin)) return false;

  res.setHeader('Access-Control-Allow-Origin', requestOrigin);
  res.setHeader('Vary', 'Origin');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  return true;
}

function retryDelayMs(response) {
  const raw = response.headers.get('retry-after');
  if (raw) {
    const seconds = Number(raw);
    if (Number.isFinite(seconds)) return Math.max(60_000, seconds * 1000);
    const date = Date.parse(raw);
    if (Number.isFinite(date)) return Math.max(60_000, date - Date.now());
  }
  return 60_000;
}

async function fetchExplorerSerialized(url, init) {
  const task = async () => {
    const waitMs = cooldownUntil - Date.now();
    if (waitMs > 0) await sleep(waitMs);

    const response = await fetch(url, init);
    if (response.status === 429) {
      cooldownUntil = Math.max(cooldownUntil, Date.now() + retryDelayMs(response));
    }
    return response;
  };

  // One upstream request at a time per running server instance.
  const promise = upstreamTail.then(task, task);
  upstreamTail = promise.then(() => undefined, () => undefined);
  return promise;
}

/**
 * Generic Node/Express/Vercel-style handler.
 * Keep LICHESS_TOKEN server-side. Never expose it in GambitDrame's browser bundle.
 *
 * Same-origin requests work without CORS. If the frontend and API are hosted on
 * different origins, set GAMBITDRAME_ALLOWED_ORIGIN to the allowed origin(s),
 * comma-separated.
 */
export async function lichessExplorerProxy(req, res) {
  const corsAllowed = applyCors(req, res);

  if (req.method === 'OPTIONS') {
    if (!corsAllowed) {
      res.statusCode = 403;
      return res.end('Origin not allowed');
    }
    res.statusCode = 204;
    return res.end();
  }

  if (!corsAllowed) {
    res.statusCode = 403;
    return res.end('Origin not allowed');
  }

  if (req.method !== 'GET') {
    res.statusCode = 405;
    res.setHeader('Allow', 'GET, OPTIONS');
    return res.end('Method Not Allowed');
  }

  const token = process.env.LICHESS_TOKEN;
  if (!token) {
    res.statusCode = 503;
    res.setHeader('Cache-Control', 'no-store');
    return res.end('LICHESS_TOKEN is not configured');
  }

  const origin = `http://${req.headers.host || 'localhost'}`;
  const incoming = new URL(req.url || '/', origin);
  const params = copyAllowedParams(incoming.searchParams);

  // Protect the upstream API from accidental abusive values.
  const moves = Math.min(50, Math.max(1, Number(params.get('moves') || 12)));
  const topGames = Math.min(4, Math.max(0, Number(params.get('topGames') || 4)));
  const recentGames = Math.min(4, Math.max(0, Number(params.get('recentGames') || 4)));
  params.set('moves', String(moves));
  params.set('topGames', String(topGames));
  params.set('recentGames', String(recentGames));

  try {
    const upstream = await fetchExplorerSerialized(`${EXPLORER_URL}?${params.toString()}`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${token}`,
        'User-Agent': 'GambitDrame/1.0',
      },
    });

    const body = await upstream.text();
    res.statusCode = upstream.status;
    res.setHeader('Content-Type', upstream.headers.get('content-type') || 'application/json; charset=utf-8');

    const retryAfter = upstream.headers.get('retry-after');
    if (retryAfter) res.setHeader('Retry-After', retryAfter);

    if (upstream.ok) {
      // Public aggregate chess data: short cache to reduce needless repeated calls.
      res.setHeader('Cache-Control', 'public, max-age=60, stale-while-revalidate=300');
    } else {
      res.setHeader('Cache-Control', 'no-store');
    }

    return res.end(body);
  } catch (error) {
    console.error('Lichess Explorer proxy failure', error);
    res.statusCode = 502;
    res.setHeader('Cache-Control', 'no-store');
    return res.end('Lichess Explorer unavailable');
  }
}

export default lichessExplorerProxy;
