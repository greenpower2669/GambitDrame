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

function copyAllowedParams(source) {
  const target = new URLSearchParams();
  for (const [key, value] of source.entries()) {
    if (ALLOWED.has(key) && value !== '') target.append(key, value);
  }
  return target;
}

function setCors(res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
}

/**
 * Generic Node/Express/Vercel-style handler.
 * Keep LICHESS_TOKEN server-side. Never expose it in GambitDrame's browser bundle.
 */
export async function lichessExplorerProxy(req, res) {
  setCors(res);

  if (req.method === 'OPTIONS') {
    res.statusCode = 204;
    return res.end();
  }

  if (req.method !== 'GET') {
    res.statusCode = 405;
    res.setHeader('Allow', 'GET, OPTIONS');
    return res.end('Method Not Allowed');
  }

  const token = process.env.LICHESS_TOKEN;
  if (!token) {
    res.statusCode = 503;
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
    const upstream = await fetch(`${EXPLORER_URL}?${params.toString()}`, {
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

    // Small cache for successful public aggregate data; never cache errors.
    if (upstream.ok) {
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
