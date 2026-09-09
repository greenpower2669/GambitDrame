import test from 'node:test';
import assert from 'node:assert/strict';

import { lichessExplorerProxy } from '../server/lichess-explorer-proxy.mjs';

function makeRes() {
  return {
    statusCode: 200,
    headers: new Map(),
    body: '',
    setHeader(name, value) {
      this.headers.set(String(name).toLowerCase(), String(value));
    },
    end(body = '') {
      this.body = String(body);
      return this.body;
    },
  };
}

function makeReq(overrides = {}) {
  return {
    method: 'GET',
    url: '/api/lichess-explorer?play=d2d4&moves=999&topGames=99',
    headers: { host: 'gambitdrame.example' },
    ...overrides,
  };
}

async function withEnv(values, fn) {
  const previous = {};
  for (const [key, value] of Object.entries(values)) {
    previous[key] = process.env[key];
    if (value === undefined) delete process.env[key];
    else process.env[key] = value;
  }
  try {
    return await fn();
  } finally {
    for (const [key, value] of Object.entries(previous)) {
      if (value === undefined) delete process.env[key];
      else process.env[key] = value;
    }
  }
}

test('same-origin request never exposes a missing token', async () => {
  await withEnv({ LICHESS_TOKEN: undefined, GAMBITDRAME_ALLOWED_ORIGIN: undefined }, async () => {
    const res = makeRes();
    await lichessExplorerProxy(makeReq(), res);
    assert.equal(res.statusCode, 503);
    assert.equal(res.headers.has('access-control-allow-origin'), false);
    assert.match(res.body, /LICHESS_TOKEN/);
  });
});

test('unknown cross-origin browser is rejected', async () => {
  await withEnv({ LICHESS_TOKEN: 'secret', GAMBITDRAME_ALLOWED_ORIGIN: undefined }, async () => {
    const res = makeRes();
    const req = makeReq({
      headers: { host: 'gambitdrame.example', origin: 'https://evil.example' },
    });
    await lichessExplorerProxy(req, res);
    assert.equal(res.statusCode, 403);
    assert.equal(res.body, 'Origin not allowed');
  });
});

test('configured frontend origin can use the proxy and upstream limits are clamped', async () => {
  const originalFetch = globalThis.fetch;
  let calledUrl = '';
  let calledAuth = '';

  globalThis.fetch = async (url, init) => {
    calledUrl = String(url);
    calledAuth = init?.headers?.Authorization || '';
    return {
      status: 200,
      ok: true,
      headers: { get: () => null },
      text: async () => JSON.stringify({ white: 1, draws: 0, black: 1, moves: [], topGames: [] }),
    };
  };

  try {
    await withEnv({
      LICHESS_TOKEN: 'secret',
      GAMBITDRAME_ALLOWED_ORIGIN: 'https://app.example',
    }, async () => {
      const res = makeRes();
      const req = makeReq({
        headers: { host: 'api.example', origin: 'https://app.example' },
      });
      await lichessExplorerProxy(req, res);

      assert.equal(res.statusCode, 200);
      assert.equal(res.headers.get('access-control-allow-origin'), 'https://app.example');
      assert.equal(calledAuth, 'Bearer secret');
      assert.match(calledUrl, /moves=50/);
      assert.match(calledUrl, /topGames=4/);
    });
  } finally {
    globalThis.fetch = originalFetch;
  }
});
