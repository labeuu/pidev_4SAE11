'use strict';

const axios = require('axios');
const config = require('../config');

const RETRY_DELAY_MS = 300;

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function isRetriableError(err) {
  if (axios.isAxiosError(err)) {
    if (err.code === 'ECONNABORTED') return true;
    if (err.code === 'ECONNREFUSED' || err.code === 'ETIMEDOUT' || err.code === 'ENOTFOUND') return true;
    if (!err.response) return true;
    const status = err.response.status;
    return status === 502 || status === 503 || status === 504;
  }
  if (err?.response && typeof err.response.status === 'number') {
    const status = err.response.status;
    return status === 502 || status === 503 || status === 504;
  }
  return false;
}

function mapHttpResponseError(err) {
  const e = new Error('Ollama request failed');
  e.expose = true;
  const status = err.response.status;
  const data = err.response.data;
  if (status >= 500) {
    e.statusCode = 502;
    e.message = 'Ollama returned an error';
    return e;
  }
  e.statusCode = 502;
  e.message = (typeof data === 'object' && data?.error) || `Ollama error (${status})`;
  return e;
}

function mapAxiosError(err) {
  const e = new Error('Ollama request failed');
  e.expose = true;

  if (err?.response && typeof err.response.status === 'number' && !axios.isAxiosError(err)) {
    return mapHttpResponseError(err);
  }

  if (axios.isAxiosError(err)) {
    if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
      e.statusCode = 504;
      e.message = 'Ollama request timed out';
      return e;
    }
    if (err.code === 'ECONNREFUSED' || err.code === 'ENOTFOUND' || !err.response) {
      e.statusCode = 503;
      e.message = 'Ollama server is unavailable';
      return e;
    }
    return mapHttpResponseError(err);
  }

  e.statusCode = 500;
  e.message = err.message || 'Unexpected error calling Ollama';
  return e;
}

async function generate(prompt) {
  const maxAttempts = config.ollamaRetries + 1;

  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    try {
      const url = `${config.ollamaBaseUrl}/api/generate`;
      const { data, status } = await axios.post(
        url,
        {
          model: config.ollamaModel,
          prompt,
          stream: false,
          options: {
            temperature: 0.2,
          },
        },
        {
          timeout: config.ollamaTimeoutMs,
          headers: { 'Content-Type': 'application/json' },
          validateStatus: () => true,
        }
      );

      if (status >= 400) {
        const fakeErr = new Error('HTTP error');
        fakeErr.response = { status, data };
        if (isRetriableError(fakeErr) && attempt < maxAttempts - 1) {
          await sleep(RETRY_DELAY_MS);
          continue;
        }
        throw mapAxiosError(fakeErr);
      }

      if (typeof data?.response !== 'string') {
        const e = new Error('Invalid response from Ollama');
        e.statusCode = 502;
        e.expose = true;
        throw e;
      }

      return data.response;
    } catch (err) {
      if (err.statusCode && err.expose) throw err;
      if (axios.isAxiosError(err)) {
        if (isRetriableError(err) && attempt < maxAttempts - 1) {
          await sleep(RETRY_DELAY_MS);
          continue;
        }
        throw mapAxiosError(err);
      }
      throw err;
    }
  }

  const fallback = new Error('Ollama request failed after retries');
  fallback.statusCode = 502;
  fallback.expose = true;
  throw fallback;
}

function modelIsListed(names, wanted) {
  if (!wanted || !Array.isArray(names) || names.length === 0) return false;
  if (names.includes(wanted)) return true;
  const colon = wanted.indexOf(':');
  const base = colon > 0 ? wanted.slice(0, colon) : wanted;
  return names.some((n) => n === base || (typeof n === 'string' && n.startsWith(`${base}:`)));
}

/**
 * Lightweight check for live UI: can we reach Ollama and is OLLAMA_MODEL present?
 * @returns {Promise<{ ollamaReachable: boolean, modelReady: boolean }>}
 */
async function checkLiveStatus() {
  const url = `${config.ollamaBaseUrl}/api/tags`;
  try {
    const { data, status } = await axios.get(url, {
      timeout: config.ollamaStatusTimeoutMs,
      headers: { Accept: 'application/json' },
      validateStatus: () => true,
    });
    if (status >= 400) {
      return { ollamaReachable: false, modelReady: false };
    }
    const models = Array.isArray(data?.models) ? data.models : [];
    const names = models
      .map((m) => (m && typeof m.name === 'string' ? m.name.trim() : ''))
      .filter(Boolean);
    const modelReady = modelIsListed(names, config.ollamaModel);
    return { ollamaReachable: true, modelReady };
  } catch (_err) {
    return { ollamaReachable: false, modelReady: false };
  }
}

module.exports = {
  generate,
  checkLiveStatus,
};
