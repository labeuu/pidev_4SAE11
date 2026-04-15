package com.esprit.aimodel.service;

import com.esprit.aimodel.exception.AiUpstreamException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class LlmGenerationService {

    private static final int RETRY_DELAY_MS = 300;
    private static final int MAX_ATTEMPTS = 2;

    private final ChatClient chatClient;

    public LlmGenerationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * @param maxOutputTokens optional {@link OllamaChatOptions#getNumPredict()} cap; speeds up short coach / Q&amp;A paths
     */
    public String generate(String prompt, Integer maxOutputTokens) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                var request = chatClient.prompt().user(prompt);
                if (maxOutputTokens != null && maxOutputTokens > 0) {
                    int capped = Math.min(maxOutputTokens, 16_384);
                    request = request.options(OllamaChatOptions.builder().numPredict(capped).build());
                }
                String content = request.call().content();
                if (content == null || content.isBlank()) {
                    throw new AiUpstreamException(HttpStatus.BAD_GATEWAY, "Invalid response from AI provider");
                }
                return content;
            } catch (AiUpstreamException e) {
                if (!isRetriableStatus(e.getStatus()) || attempt == MAX_ATTEMPTS - 1) {
                    throw e;
                }
                sleepRetry();
            } catch (Exception e) {
                AiUpstreamException mapped = mapException(e);
                if (!isRetriable(e) || attempt == MAX_ATTEMPTS - 1) {
                    throw mapped;
                }
                sleepRetry();
            }
        }
        throw new AiUpstreamException(HttpStatus.BAD_GATEWAY, "AI request failed after retries");
    }

    private static boolean isRetriableStatus(HttpStatus status) {
        return status == HttpStatus.BAD_GATEWAY
                || status == HttpStatus.SERVICE_UNAVAILABLE
                || status == HttpStatus.GATEWAY_TIMEOUT;
    }

    private static boolean isRetriable(Exception e) {
        if (e instanceof ResourceAccessException) {
            return true;
        }
        if (e instanceof RestClientResponseException r) {
            int s = r.getStatusCode().value();
            return s == 502 || s == 503 || s == 504;
        }
        Throwable t = e;
        while (t != null) {
            if (t instanceof ConnectException
                    || t instanceof SocketTimeoutException
                    || t instanceof UnknownHostException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private static AiUpstreamException mapException(Exception e) {
        if (e instanceof AiUpstreamException u) {
            return u;
        }
        Throwable t = e;
        while (t != null) {
            if (t instanceof ConnectException || t instanceof UnknownHostException) {
                return new AiUpstreamException(HttpStatus.SERVICE_UNAVAILABLE, "AI provider is unavailable");
            }
            if (t instanceof SocketTimeoutException) {
                return new AiUpstreamException(HttpStatus.GATEWAY_TIMEOUT, "AI provider request timed out");
            }
            t = t.getCause();
        }
        if (e instanceof ResourceAccessException) {
            String m = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (m.contains("timeout") || m.contains("timed out")) {
                return new AiUpstreamException(HttpStatus.GATEWAY_TIMEOUT, "AI provider request timed out");
            }
            return new AiUpstreamException(HttpStatus.SERVICE_UNAVAILABLE, "AI provider is unavailable");
        }
        if (e instanceof RestClientResponseException r) {
            int s = r.getStatusCode().value();
            if (s == 408 || s == 504) {
                return new AiUpstreamException(HttpStatus.GATEWAY_TIMEOUT, "AI provider request timed out");
            }
            if (s == 503) {
                return new AiUpstreamException(HttpStatus.SERVICE_UNAVAILABLE, "AI provider returned an error");
            }
            if (s >= 500) {
                return new AiUpstreamException(HttpStatus.BAD_GATEWAY, "AI provider returned an error");
            }
            return new AiUpstreamException(HttpStatus.BAD_GATEWAY, extractClientMessage(r));
        }
        String msg = e.getMessage() != null ? e.getMessage() : "Unexpected error calling AI provider";
        return new AiUpstreamException(HttpStatus.BAD_GATEWAY, msg);
    }

    private static String extractClientMessage(RestClientResponseException r) {
        try {
            String body = r.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                return body.length() > 500 ? body.substring(0, 500) + "..." : body;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "AI provider error (" + r.getStatusCode().value() + ")";
    }

    private static void sleepRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AiUpstreamException(HttpStatus.BAD_GATEWAY, "Interrupted during AI retry");
        }
    }
}
