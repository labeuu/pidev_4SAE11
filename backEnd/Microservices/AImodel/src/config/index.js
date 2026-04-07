'use strict';

function parseIntEnv(name, defaultValue) {
  const raw = process.env[name];
  if (raw === undefined || raw === '') return defaultValue;
  const n = Number.parseInt(raw, 10);
  return Number.isFinite(n) ? n : defaultValue;
}

const eurekaAppName = (process.env.EUREKA_APP_NAME || 'AIMODEL').trim().toUpperCase();

const config = {
  port: parseIntEnv('PORT', 8092),
  ollamaBaseUrl: (process.env.OLLAMA_BASE_URL || 'http://localhost:11434').replace(/\/$/, ''),
  ollamaModel: process.env.OLLAMA_MODEL || 'qwen3:8b',
  // Default 4h for slow local models; override with OLLAMA_TIMEOUT_MS if needed.
  ollamaTimeoutMs: parseIntEnv('OLLAMA_TIMEOUT_MS', 14400000),
  /** Fast probe for GET /api/ai/status (tags list). */
  ollamaStatusTimeoutMs: parseIntEnv('OLLAMA_STATUS_TIMEOUT_MS', 5000),
  ollamaRetries: 1,
  eurekaEnabled: process.env.EUREKA_ENABLED !== 'false',
  eurekaServerUrl: (process.env.EUREKA_SERVER_URL || 'http://localhost:8420/eureka').replace(/\/$/, ''),
  eurekaAppName,
  eurekaInstanceHostname: process.env.EUREKA_INSTANCE_HOSTNAME || 'localhost',
  eurekaInstanceIpAddr: process.env.EUREKA_INSTANCE_IP || '127.0.0.1',
  eurekaRenewalIntervalSecs: parseIntEnv('EUREKA_RENEWAL_INTERVAL_SECS', 30),
  eurekaDurationSecs: parseIntEnv('EUREKA_DURATION_SECS', 90),
};

module.exports = config;
