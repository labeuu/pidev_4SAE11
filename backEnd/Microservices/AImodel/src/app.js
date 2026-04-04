'use strict';

const express = require('express');
const aiRoutes = require('./routes/aiRoutes');

function createApp() {
  const app = express();

  app.use(express.json());

  app.get('/actuator/health', (req, res) => {
    res.status(200).json({ status: 'UP' });
  });

  app.use('/api/ai', aiRoutes);

  app.use((req, res) => {
    res.status(404).json({
      success: false,
      error: { message: 'Not found' },
    });
  });

  app.use((err, req, res, next) => {
    if (err instanceof SyntaxError && err.status === 400 && 'body' in err) {
      return res.status(400).json({
        success: false,
        error: { message: 'Invalid JSON body' },
      });
    }
    const statusCode = err.statusCode && Number.isFinite(err.statusCode) ? err.statusCode : 500;
    const message =
      err.expose && typeof err.message === 'string' ? err.message : 'Internal server error';
    if (statusCode >= 500) {
      console.error('[error]', err.message);
    }
    res.status(statusCode).json({
      success: false,
      error: { message },
    });
  });

  return app;
}

module.exports = { createApp };
