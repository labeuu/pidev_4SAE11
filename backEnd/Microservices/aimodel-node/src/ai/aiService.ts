import { generate } from "./llmService.js";
import { stripModelNoise, subtaskPlannerPromptPrefix, taskPlannerPromptPrefix } from "./textSanitizer.js";

export async function generateResponse(prompt: string, maxOutputTokens?: number): Promise<string> {
  return generate(prompt, maxOutputTokens);
}

export async function generateTasks(context: string): Promise<string> {
  const result = await generate(`${taskPlannerPromptPrefix()}${context}`);
  return stripModelNoise(result) ?? "";
}

export async function generateSubtasks(context: string): Promise<string> {
  const result = await generate(`${subtaskPlannerPromptPrefix()}${context}`);
  return stripModelNoise(result) ?? "";
}
