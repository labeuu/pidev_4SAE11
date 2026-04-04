'use strict';

const ollamaClient = require('../clients/ollamaClient');

/** LLMs often wrap JSON in ``` fences despite instructions. */
function stripModelNoise(text) {
  if (typeof text !== 'string') return text;
  let s = text.trim();
  if (s.startsWith('```')) {
    const nl = s.indexOf('\n');
    if (nl !== -1) {
      s = s.slice(nl + 1);
      const end = s.lastIndexOf('```');
      if (end > 0) s = s.slice(0, end);
    }
  }
  return s.trim();
}

function todayYyyyMmDd() {
  return new Date().toISOString().slice(0, 10);
}

function taskPlannerPromptPrefix() {
  const today = todayYyyyMmDd();
  return `You are a task planner. Return ONLY valid JSON with no markdown fences or commentary.
The JSON must be an object with a single key "tasks" whose value is an array of objects.
Each task object must have these string fields: "title", "description", "priority" (one of "low", "medium", "high"), and "dueDate" (deadline as yyyy-MM-dd).
Scheduling: today is ${today}. Use dueDate values on or after ${today}. Spread deadlines across the next few weeks (not all the same day) according to realistic effort order.
Example shape: {"tasks":[{"title":"...","description":"...","priority":"medium","dueDate":"${today}"}]}

User context to plan tasks for:
`;
}

function subtaskPlannerPromptPrefix() {
  const today = todayYyyyMmDd();
  return `You are a task breakdown assistant. Return ONLY valid JSON with no markdown fences or commentary.
The JSON must be an object with a single key "subtasks" whose value is an array of objects.
Each subtask object must have these string fields: "title", "description", "priority" (one of "low", "medium", "high"), and "dueDate" (yyyy-MM-dd).
Break the parent task into concrete, ordered subtasks a freelancer can execute. Use chronological dueDate values on or after ${today}, spaced by a few days when logical.
Example shape: {"subtasks":[{"title":"...","description":"...","priority":"medium","dueDate":"${today}"}]}

Parent task and context to decompose:
`;
}

async function generateResponse(prompt) {
  console.log('[aiService] generateResponse: request received');
  try {
    const text = await ollamaClient.generate(prompt);
    console.log('[aiService] generateResponse: completed');
    return text;
  } catch (err) {
    console.error('[aiService] generateResponse failed:', err.message);
    throw err;
  }
}

async function generateTasks(context) {
  console.log('[aiService] generateTasks: request received');
  const prompt = `${taskPlannerPromptPrefix()}${context}`;
  try {
    const text = stripModelNoise(await ollamaClient.generate(prompt));
    console.log('[aiService] generateTasks: completed');
    return text;
  } catch (err) {
    console.error('[aiService] generateTasks failed:', err.message);
    throw err;
  }
}

async function generateSubtasks(context) {
  console.log('[aiService] generateSubtasks: request received');
  const prompt = `${subtaskPlannerPromptPrefix()}${context}`;
  try {
    const text = stripModelNoise(await ollamaClient.generate(prompt));
    console.log('[aiService] generateSubtasks: completed');
    return text;
  } catch (err) {
    console.error('[aiService] generateSubtasks failed:', err.message);
    throw err;
  }
}

module.exports = {
  generateResponse,
  generateTasks,
  generateSubtasks,
};
