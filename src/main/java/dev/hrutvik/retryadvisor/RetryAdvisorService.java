package dev.hrutvik.retryadvisor;

import org.springframework.stereotype.Service;

@Service
public class RetryAdvisorService {

    public RetryDecision advise(RetryRequest request) {
        int status = request.httpStatus();
        String operation = request.operationType().toUpperCase();
        boolean protectedWrite = request.hasIdempotencyKey();

        if (request.attemptNumber() >= 5) {
            return new RetryDecision("DO_NOT_RETRY", 0, 5, "HIGH",
                    "Maximum retry attempts reached");
        }

        if (status == 429) {
            return new RetryDecision("RETRY", backoff(request.attemptNumber()), 5, "MEDIUM",
                    "Rate limited; retry with exponential backoff and jitter");
        }

        if (status == 408 || status == 502 || status == 503 || status == 504) {
            if (isRiskyWrite(operation) && !protectedWrite) {
                return new RetryDecision("DO_NOT_RETRY", 0, 1, "HIGH",
                        "Retry could duplicate a write; provide an idempotency key first");
            }
            return new RetryDecision("RETRY", backoff(request.attemptNumber()), 5, "LOW",
                    "Temporary upstream failure and retry is safe");
        }

        if (status >= 500) {
            return new RetryDecision("REVIEW", 0, 1, "MEDIUM",
                    "Server failure may be transient, but this status needs explicit policy");
        }

        return new RetryDecision("DO_NOT_RETRY", 0, 1, "LOW",
                "Client or business errors should be fixed instead of retried");
    }

    private boolean isRiskyWrite(String operation) {
        return operation.equals("PAYMENT") || operation.equals("ORDER_CREATE") || operation.equals("REFUND");
    }

    private int backoff(int attempt) {
        return Math.min(60, (int) Math.pow(2, Math.max(1, attempt)));
    }

    public record RetryRequest(
            int httpStatus,
            String operationType,
            int attemptNumber,
            boolean hasIdempotencyKey,
            String errorMessage) {}

    public record RetryDecision(
            String decision,
            int retryAfterSeconds,
            int maxAttempts,
            String risk,
            String reason) {}
}
