# Retry Advisor API

A small Java 17 and Spring Boot service that helps backend systems decide whether a failed operation should be retried safely.

## Problem

Blind retries can create duplicate payments, duplicate orders, and retry storms. Different failures also require different strategies: a `400` should not be retried, while a `503` may be temporary.

## Solution

The API evaluates the HTTP status, operation type, retry attempt, and idempotency protection, then returns:

- retry decision
- recommended delay
- maximum attempts
- operational risk
- human-readable reason

## API

`POST /api/v1/retries/advise`

```json
{
  "httpStatus": 503,
  "operationType": "PAYMENT",
  "attemptNumber": 2,
  "hasIdempotencyKey": true,
  "errorMessage": "Upstream service unavailable"
}
```

Response:

```json
{
  "decision": "RETRY",
  "retryAfterSeconds": 4,
  "maxAttempts": 5,
  "risk": "LOW",
  "reason": "Temporary upstream failure and retry is safe"
}
```

## Main Rules

| Situation | Decision |
|---|---|
| `400` and other client errors | Do not retry |
| `408`, `502`, `503`, `504` | Retry when safe |
| `429` | Retry with backoff |
| Payment/order/refund without idempotency | Block retry |
| Five attempts already used | Stop retrying |
| Unclassified `5xx` | Manual policy review |

## Run Locally

```bash
mvn spring-boot:run
```

Or with Docker:

```bash
docker build -t retry-advisor-api .
docker run -p 8080:8080 retry-advisor-api
```

Test it:

```bash
curl -X POST http://localhost:8080/api/v1/retries/advise \
  -H 'Content-Type: application/json' \
  -d '{
    "httpStatus": 503,
    "operationType": "PAYMENT",
    "attemptNumber": 2,
    "hasIdempotencyKey": true,
    "errorMessage": "Upstream unavailable"
  }'
```

## Engineering Decisions

- Deterministic rules instead of unnecessary AI
- Exponential backoff capped at 60 seconds
- Idempotency awareness for risky write operations
- Explicit `REVIEW` outcome when policy is ambiguous
- Stateless API that can scale horizontally
- Automated tests and GitHub Actions CI

## Possible Extensions

- Add jitter to backoff values
- Support configurable policies per service
- Store policy versions and audit decisions
- Export Prometheus metrics
- Publish a Java client library

## Tech Stack

Java 17, Spring Boot, Jakarta Validation, Maven, Docker, JUnit 5, GitHub Actions.
