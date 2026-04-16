import type { NextFunction, Request, Response } from "express";
import { createRemoteJWKSet, jwtVerify, type JWTVerifyOptions } from "jose";
import { config } from "../config.js";
import { toErrorEnvelope } from "../errors.js";
import { isAuthExempt } from "./gatewayOnly.js";

export async function jwtAuthMiddleware(request: Request, response: Response, next: NextFunction): Promise<void> {
  if (isAuthExempt(request)) {
    next();
    return;
  }

  if (!config.jwtIssuer || !config.jwtJwksUri) {
    response.status(500).json(toErrorEnvelope("JWT authentication is not configured"));
    return;
  }

  const authorization = request.header("Authorization");
  if (!authorization?.startsWith("Bearer ")) {
    response.status(401).json(toErrorEnvelope("Missing bearer token"));
    return;
  }

  try {
    const jwks = createRemoteJWKSet(new URL(config.jwtJwksUri));
    const token = authorization.slice("Bearer ".length).trim();
    const options: JWTVerifyOptions = {
      issuer: config.jwtIssuer,
    };

    if (config.jwtAudience) {
      options.audience = config.jwtAudience;
    }

    const verified = await jwtVerify(token, jwks, options);
    request.auth = verified.payload;
    next();
  } catch {
    response.status(401).json(toErrorEnvelope("Invalid bearer token"));
  }
}
