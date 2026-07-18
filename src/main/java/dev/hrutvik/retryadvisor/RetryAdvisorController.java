package dev.hrutvik.retryadvisor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/retries")
public class RetryAdvisorController {

    private final RetryAdvisorService retryAdvisorService;

    public RetryAdvisorController(RetryAdvisorService retryAdvisorService) {
        this.retryAdvisorService = retryAdvisorService;
    }

    @PostMapping("/advise")
    public RetryAdvisorService.RetryDecision advise(@Valid @RequestBody RetryAdviceRequest request) {
        return retryAdvisorService.advise(new RetryAdvisorService.RetryRequest(
                request.httpStatus(),
                request.operationType(),
                request.attemptNumber(),
                request.hasIdempotencyKey(),
                request.errorMessage()));
    }

    public record RetryAdviceRequest(
            @Min(100) @Max(599) int httpStatus,
            @NotBlank String operationType,
            @Min(1) int attemptNumber,
            boolean hasIdempotencyKey,
            String errorMessage) {}
}
