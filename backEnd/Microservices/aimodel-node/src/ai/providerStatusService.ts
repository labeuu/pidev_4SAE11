import axios from "axios";
import { config, getRequestTimeoutMs } from "../config.js";
import type { AiLiveStatus } from "../dto.js";

function parseOllamaModelNames(payload: unknown): string[] {
  if (!payload || typeof payload !== "object") {
    return [];
  }

  const models = (payload as { models?: unknown }).models;
  if (!Array.isArray(models)) {
    return [];
  }

  return models
    .map((model) => {
      if (!model || typeof model !== "object") {
        return undefined;
      }
      const name = (model as { name?: unknown }).name;
      return typeof name === "string" ? name.trim() : undefined;
    })
    .filter((name): name is string => Boolean(name));
}

function modelIsListed(names: string[], wanted: string): boolean {
  if (!wanted.trim() || names.length === 0) {
    return false;
  }

  if (names.includes(wanted)) {
    return true;
  }

  const base = wanted.includes(":") ? wanted.slice(0, wanted.indexOf(":")) : wanted;
  return names.some((name) => name === base || name.startsWith(`${base}:`));
}

export async function liveStatus(): Promise<AiLiveStatus> {
  try {
    const response = await axios.get(`${config.ollamaBaseUrl}/api/tags`, {
      timeout: getRequestTimeoutMs(),
      headers: { Accept: "application/json" },
    });

    const names = parseOllamaModelNames(response.data);
    return {
      service: config.serviceName,
      status: "UP",
      ollamaReachable: true,
      model: config.ollamaModel,
      modelReady: modelIsListed(names, config.ollamaModel),
    };
  } catch {
    return {
      service: config.serviceName,
      status: "UP",
      ollamaReachable: false,
      model: config.ollamaModel,
      modelReady: false,
    };
  }
}
