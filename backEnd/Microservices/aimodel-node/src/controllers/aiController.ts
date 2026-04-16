import { Router } from "express";
import { z } from "zod";
import type { AiGenerateResponse } from "../dto.js";
import { generateResponse, generateSubtasks, generateTasks } from "../ai/aiService.js";
import { liveStatus } from "../ai/providerStatusService.js";

const promptSchema = z.object({
  prompt: z.string().trim().min(1, "prompt is required"),
  maxOutputTokens: z.number().int().positive().optional(),
});

const contextSchema = z.object({
  context: z.string().trim().min(1, "context is required"),
});

export const aiRouter = Router();

aiRouter.get("/status", async (_request, response, next) => {
  try {
    response.json(await liveStatus());
  } catch (error) {
    next(error);
  }
});

aiRouter.post("/generate", async (request, response, next) => {
  try {
    const body = promptSchema.parse(request.body);
    const data = await generateResponse(body.prompt, body.maxOutputTokens);
    const payload: AiGenerateResponse = { success: true, data };
    response.json(payload);
  } catch (error) {
    next(error);
  }
});

aiRouter.post("/generate-tasks", async (request, response, next) => {
  try {
    const body = contextSchema.parse(request.body);
    const data = await generateTasks(body.context);
    const payload: AiGenerateResponse = { success: true, data };
    response.json(payload);
  } catch (error) {
    next(error);
  }
});

aiRouter.post("/generate-subtasks", async (request, response, next) => {
  try {
    const body = contextSchema.parse(request.body);
    const data = await generateSubtasks(body.context);
    const payload: AiGenerateResponse = { success: true, data };
    response.json(payload);
  } catch (error) {
    next(error);
  }
});
