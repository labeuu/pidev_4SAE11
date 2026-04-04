'use strict';

const aiService = require('../services/aiService');
const ollamaClient = require('../clients/ollamaClient');
const config = require('../config');

function validationError(message) {
  const err = new Error(message);
  err.statusCode = 400;
  err.expose = true;
  return err;
}

function normalizeString(value, fieldName) {
  if (value === undefined || value === null) {
    throw validationError(`${fieldName} is required`);
  }
  if (typeof value !== 'string') {
    throw validationError(`${fieldName} must be a string`);
  }
  const trimmed = value.trim();
  if (!trimmed) {
    throw validationError(`${fieldName} cannot be empty`);
  }
  return trimmed;
}

async function generate(req, res, next) {
  try {
    const prompt = normalizeString(req.body?.prompt, 'prompt');
    const data = await aiService.generateResponse(prompt);
    res.status(200).json({ success: true, data });
  } catch (err) {
    next(err);
  }
}

async function generateTasks(req, res, next) {
  try {
    const context = normalizeString(req.body?.context, 'context');
    const data = await aiService.generateTasks(context);
    res.status(200).json({ success: true, data });
  } catch (err) {
    next(err);
  }
}

async function generateSubtasks(req, res, next) {
  try {
    const context = normalizeString(req.body?.context, 'context');
    const data = await aiService.generateSubtasks(context);
    res.status(200).json({ success: true, data });
  } catch (err) {
    next(err);
  }
}

async function status(req, res, next) {
  try {
    const { ollamaReachable, modelReady } = await ollamaClient.checkLiveStatus();
    res.status(200).json({
      service: 'aimodel',
      status: 'UP',
      ollamaReachable,
      model: config.ollamaModel,
      modelReady: ollamaReachable && modelReady,
    });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  generate,
  generateTasks,
  generateSubtasks,
  status,
};
