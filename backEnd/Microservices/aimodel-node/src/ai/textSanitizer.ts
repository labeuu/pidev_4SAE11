function todayYyyyMmDd(): string {
  return new Date().toISOString().slice(0, 10);
}

export function stripModelNoise(text: string | null | undefined): string | null | undefined {
  if (text == null) {
    return text;
  }

  let value = text.trim();
  if (value.startsWith("```")) {
    const firstNewline = value.indexOf("\n");
    if (firstNewline !== -1) {
      value = value.slice(firstNewline + 1);
      const closingFence = value.lastIndexOf("```");
      if (closingFence > 0) {
        value = value.slice(0, closingFence);
      }
    }
  }

  return value.trim();
}

export function taskPlannerPromptPrefix(): string {
  const today = todayYyyyMmDd();
  return (
    "You are a task planner. Return ONLY valid JSON with no markdown fences or commentary.\n"
    + 'The JSON must be an object with a single key "tasks" whose value is an array of objects.\n'
    + 'Each task object must have these string fields: "title", "description", "priority" (one of "low", "medium", "high"), and "dueDate" (deadline as yyyy-MM-dd).\n'
    + `Scheduling: today is ${today}. Use dueDate values on or after ${today}. Spread deadlines across the next few weeks (not all the same day) according to realistic effort order.\n`
    + `Example shape: {"tasks":[{"title":"...","description":"...","priority":"medium","dueDate":"${today}"}]}\n`
    + "\n"
    + "User context to plan tasks for:\n"
  );
}

export function subtaskPlannerPromptPrefix(): string {
  const today = todayYyyyMmDd();
  return (
    "You are a task breakdown assistant. Return ONLY valid JSON with no markdown fences or commentary.\n"
    + 'The JSON must be an object with a single key "subtasks" whose value is an array of objects.\n'
    + 'Each subtask object must have these string fields: "title", "description", "priority" (one of "low", "medium", "high"), and "dueDate" (yyyy-MM-dd).\n'
    + `Break the parent task into concrete, ordered subtasks a freelancer can execute. Use chronological dueDate values on or after ${today}, spaced by a few days when logical.\n`
    + `Example shape: {"subtasks":[{"title":"...","description":"...","priority":"medium","dueDate":"${today}"}]}\n`
    + "\n"
    + "Parent task and context to decompose:\n"
  );
}
