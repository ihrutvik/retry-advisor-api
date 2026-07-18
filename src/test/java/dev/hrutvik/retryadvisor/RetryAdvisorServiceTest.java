package dev.hrutvik.retryadvisor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetryAdvisorServiceTest {

    private final RetryAdvisorService service = new RetryAdvisorService();

    @Test
    void retriesTemporaryFailureWhenWriteIsProtected() {
        var result = service.advise(new RetryAdvisorService.RetryRequest(
                503, "PAYMENT", 2, true, "upstream unavailable"));

        assertThat(result.decision()).isEqualTo("RETRY");
        assertThat(result.retryAfterSeconds()).isEqualTo(4);
        assertThat(result.risk()).isEqualTo("LOW");
    }

    @Test
    void blocksUnsafePaymentRetryWithoutIdempotency() {
        var result = service.advise(new RetryAdvisorService.RetryRequest(
                504, "PAYMENT", 1, false, "timeout"));

        assertThat(result.decision()).isEqualTo("DO_NOT_RETRY");
        assertThat(result.risk()).isEqualTo("HIGH");
    }

    @Test
    void doesNotRetryClientError() {
        var result = service.advise(new RetryAdvisorService.RetryRequest(
                400, "READ", 1, false, "invalid input"));

        assertThat(result.decision()).isEqualTo("DO_NOT_RETRY");
    }
}
