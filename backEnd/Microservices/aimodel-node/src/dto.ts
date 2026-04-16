export interface AiPromptRequest {
  prompt: string;
  maxOutputTokens?: number;
}

export interface AiContextRequest {
  context: string;
}

export interface AiGenerateResponse {
  success: boolean;
  data: string;
}

export interface AiLiveStatus {
  service: string;
  status: string;
  ollamaReachable: boolean;
  model: string;
  modelReady: boolean;
}

export interface AiErrorEnvelope {
  success: false;
  error: {
    message: string;
  };
}
