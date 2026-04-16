import dotenv from "dotenv";

dotenv.config();

function readString(name: string, fallback: string): string {
  const value = process.env[name];
  return value && value.trim() ? value.trim() : fallback;
}

function readNumber(name: string, fallback: number): number {
  const raw = process.env[name];
  if (!raw) {
    return fallback;
  }

  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function readOptionalString(name: string): string | undefined {
  const value = process.env[name];
  return value && value.trim() ? value.trim() : undefined;
}

export const config = {
  appName: readString("EUREKA_APP_NAME", "AIMODEL"),
  serviceName: "aimodel",
  port: readNumber("PORT", readNumber("SERVER_PORT", 8095)),
  nodeEnv: readString("NODE_ENV", "development"),
  ollamaBaseUrl: readString("OLLAMA_BASE_URL", "http://localhost:11434").replace(/\/+$/, ""),
  ollamaModel: readString("OLLAMA_MODEL", "gemma3:4b"),
  ollamaTemperature: Number(readString("OLLAMA_TEMPERATURE", "0.2")),
  ollamaKeepAlive: readString("OLLAMA_KEEP_ALIVE", "15m"),
  connectTimeoutMs: readNumber("AIMODEL_CONNECT_TIMEOUT_MS", 5000),
  readTimeoutMs: readNumber("AIMODEL_READ_TIMEOUT_MS", 5000),
  generateTimeoutMs: readNumber("AIMODEL_GENERATE_TIMEOUT_MS", 14400000),
  eurekaBaseUrl: readString(
    "EUREKA_CLIENT_SERVICE_URL",
    readString("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", "http://localhost:8420/eureka"),
  ).replace(/\/+$/, ""),
  eurekaHostName: readOptionalString("EUREKA_INSTANCE_HOSTNAME") ?? readOptionalString("HOSTNAME") ?? "localhost",
  eurekaPreferIpAddress: readString("EUREKA_INSTANCE_PREFER_IP_ADDRESS", "false").toLowerCase() === "true",
  jwtIssuer: readOptionalString("KEYCLOAK_ISSUER_URI")
    ?? readOptionalString("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI"),
  jwtJwksUri: readOptionalString("KEYCLOAK_JWKS_URI")
    ?? readOptionalString("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI"),
  jwtAudience: readOptionalString("JWT_AUDIENCE"),
};

export function getPublicHost(): string {
  return config.eurekaHostName;
}

export function getRequestTimeoutMs(): number {
  return Math.max(config.connectTimeoutMs, config.readTimeoutMs);
}
