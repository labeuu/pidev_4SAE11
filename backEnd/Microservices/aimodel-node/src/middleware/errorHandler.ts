import type { NextFunction, Request, Response } from "express";
import { ZodError } from "zod";
import { ApiError, toErrorEnvelope } from "../errors.js";

export function notFoundHandler(_request: Request, response: Response): void {
  response.status(404).json(toErrorEnvelope("Not found"));
}

export function errorHandler(error: unknown, _request: Request, response: Response, _next: NextFunction): void {
  if (error instanceof SyntaxError && "body" in error) {
    response.status(400).json(toErrorEnvelope("Invalid JSON body"));
    return;
  }

  if (error instanceof ZodError) {
    const message = error.issues[0]?.message ?? "Validation error";
    response.status(400).json(toErrorEnvelope(message));
    return;
  }

  if (error instanceof ApiError) {
    response.status(error.status).json(toErrorEnvelope(error.message));
    return;
  }

  if (error instanceof Error) {
    response.status(500).json(toErrorEnvelope(error.message || "Internal server error"));
    return;
  }

  response.status(500).json(toErrorEnvelope("Internal server error"));
}
