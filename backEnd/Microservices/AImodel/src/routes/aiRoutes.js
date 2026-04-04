'use strict';

const express = require('express');
const aiController = require('../controllers/aiController');

const router = express.Router();

router.get('/status', aiController.status);
router.post('/generate', aiController.generate);
router.post('/generate-tasks', aiController.generateTasks);
router.post('/generate-subtasks', aiController.generateSubtasks);

module.exports = router;
