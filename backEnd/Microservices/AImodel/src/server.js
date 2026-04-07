'use strict';

const config = require('./config');
const { createApp } = require('./app');
const eurekaClient = require('./clients/eurekaClient');

const app = createApp();

const server = app.listen(config.port, async () => {
  console.log(`AImodel listening on port ${config.port}`);
  if (config.eurekaEnabled) {
    try {
      await eurekaClient.start();
    } catch (err) {
      console.error('[eureka] registration failed:', err.message);
    }
  }
});

function shutdown(signal) {
  return async () => {
    console.log(`[server] ${signal} received, shutting down`);
    try {
      await eurekaClient.stop();
    } catch (err) {
      console.warn('[server] eureka stop:', err.message);
    }
    server.close(() => process.exit(0));
  };
}

process.on('SIGINT', shutdown('SIGINT'));
process.on('SIGTERM', shutdown('SIGTERM'));
