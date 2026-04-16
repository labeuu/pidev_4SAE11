import axios, { AxiosError } from "axios";
import { config } from "../config.js";
import { AiUpstreamError } from "../errors.js";

const MAX_ATTEMPTS = 2;
const RETRY_DELAY_MS = 300;

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function buildOptions(maxOutputTokens?: number): Record<string, number> {
  const options: Record<string, number> = {
    temperature: config.ollamaTemperature,
  };

  if (maxOutputTokens && maxOutputTokens > 0) {
    options.num_predict = Math.min(maxOutputTokens, 16384);
  }

  return options;
}

function isRetriableStatus(status?: number): boolean {
  return status === 408 || status === 502 || status === 503 || status === 504;
}

function isRetriable(error: unknown): boolean {
  if (error instanceof AiUpstreamError) {
    return isRetriableStatus(error.status);
  }

  if (axios.isAxiosError(error)) {
    return !error.response || isRetriableStatus(error.response.status);
  }

  return false;
}

function mapAxiosError(error: AxiosError): AiUpstreamError {
  const status = error.response?.status;

  if (!error.response) {
    if (error.code === "ECONNABORTED") {
      return new AiUpstreamError(504, "AI provider request timed out");
    }
    return new AiUpstreamError(503, "AI provider is unavailable");
  }

  if (status === 408 || status === 504) {
    return new AiUpstreamError(504, "AI provider request timed out");
  }

  if (status === 503) {
    return new AiUpstreamError(503, "AI provider returned an error");
  }

  if (status && status >= 500) {
    return new AiUpstreamError(502, "AI provider returned an error");
  }

  const responseBody = typeof error.response.data === "string"
    ? error.response.data
    : JSON.stringify(error.response.data);

  const message = responseBody && responseBody.trim()
    ? responseBody.slice(0, 500)
    : `AI provider error (${status ?? "unknown"})`;

  return new AiUpstreamError(502, message);
}

function mapUnknownError(error: unknown): AiUpstreamError {
  if (error instanceof AiUpstreamError) {
    return error;
  }

  if (axios.isAxiosError(error)) {
    return mapAxiosError(error);
  }

  if (error instanceof Error) {
    return new AiUpstreamError(502, error.message || "Unexpected error calling AI provider");
  }

  return new AiUpstreamError(502, "Unexpected error calling AI provider");
}

function extractContent(data: unknown): string | undefined {
  if (!data || typeof data !== "object") {
    return undefined;
  }

  const payload = data as {
    message?: { content?: unknown };
    response?: unknown;
  };

  if (payload.message && typeof payload.message.content === "string") {
    return payload.message.content;
  }

  if (typeof payload.response === "string") {
    return payload.response;
  }

  return undefined;
}

export async function generate(prompt: string, maxOutputTokens?: number): Promise<string> {
  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt += 1) {
    try {
      const response = await axios.post(
        `${config.ollamaBaseUrl}/api/chat`,
        {
          model: config.ollamaModel,
          messages: [{ role: "user", content: prompt }],
          stream: false,
          keep_alive: config.ollamaKeepAlive,
          options: buildOptions(maxOutputTokens),
        },
        {
          timeout: config.generateTimeoutMs,
          headers: { "Content-Type": "application/json" },
        },
      );

      const content = extractContent(response.data);
      if (!content || !content.trim()) {
        throw new AiUpstreamError(502, "Invalid response from AI provider");
      }

      return content;
    } catch (error) {
      const mapped = mapUnknownError(error);
      if (!isRetriable(error) || attempt === MAX_ATTEMPTS - 1) {
        throw mapped;
      }
      await sleep(RETRY_DELAY_MS);
    }
  }

  throw new AiUpstreamError(502, "AI request failed after retries");
}
