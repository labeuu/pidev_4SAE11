import type { NextFunction, Request, Response } from "express";
import { toErrorEnvelope } from "../errors.js";

const PUBLIC_PREFIXES = ["/actuator/health", "/actuator/info", "/v3/api-docs", "/swagger-ui"];

function isPublicRequest(request: Request): boolean {
  return request.method === "OPTIONS"
    || PUBLIC_PREFIXES.some((prefix) => request.path.startsWith(prefix));
}

export function gatewayOnlyMiddleware(request: Request, response: Response, next: NextFunction): void {
  if (isPublicRequest(request)) {
    next();
    return;
  }

  if (request.header("X-Internal-Gateway") !== "true") {
    response.status(403).json(toErrorEnvelope("Direct access to aimodel service is not allowed"));
    return;
  }

  next();
}

export function isAuthExempt(request: Request): boolean {
  return isPublicRequest(request);
}
