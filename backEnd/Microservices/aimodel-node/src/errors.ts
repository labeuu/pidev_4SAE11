import type { AiErrorEnvelope } from "./dto.js";

export class ApiError extends Error {
  public readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export class AiUpstreamError extends ApiError {
  constructor(status: number, message: string) {
    super(status, message);
  }
}

export function toErrorEnvelope(message: string): AiErrorEnvelope {
  return {
    success: false,
    error: { message },
  };
}
