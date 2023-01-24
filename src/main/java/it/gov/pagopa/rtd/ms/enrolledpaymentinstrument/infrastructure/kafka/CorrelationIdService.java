package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka;

import java.util.Objects;
import java.util.Optional;

public final class CorrelationIdService {

  public static CorrelationIdService create() {
    return new CorrelationIdService();
  }

  private final ThreadLocal<String> correlationId;

  private CorrelationIdService() {
    this.correlationId = new ThreadLocal<>();
  }

  public void setCorrelationId(String correlationId) {
    if (Objects.nonNull(correlationId)) {
      this.correlationId.set(correlationId);
    }
  }

  public Optional<String> popCorrelationId() {
    final var correlation = peekCorrelationId();
    correlationId.remove();
    return correlation;
  }

  public Optional<String> peekCorrelationId() {
    return Optional.ofNullable(correlationId.get());
  }
}
