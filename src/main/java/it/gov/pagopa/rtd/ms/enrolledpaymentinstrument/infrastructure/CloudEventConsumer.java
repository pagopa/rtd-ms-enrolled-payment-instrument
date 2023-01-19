package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.CorrelationIdService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@AllArgsConstructor
@Slf4j
public final class CloudEventConsumer<E, T extends CloudEvent<E>> implements Consumer<T> {

  private final CorrelationIdService correlationIdService;
  private final Consumer<T> consumer;

  @Override
  public void accept(T t) {
    try {
      correlationIdService.setCorrelationId(t.getCorrelationId());
      consumer.accept(t);
    } finally {
      correlationIdService.popCorrelationId();
    }
  }
}
