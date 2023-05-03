package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.util.function.Consumer;

@AllArgsConstructor
@Slf4j
public final class ValidatedConsumer<T> implements Consumer<T> {

  private final Validator validator;
  private final Consumer<T> consumer;

  @Override
  public void accept(T t) {
    log.info("Received message to validate {}", t);
    final var violations = validator.validate(t);
    if (violations.isEmpty()) {
      consumer.accept(t);
    } else {
      log.error("Malformed event {}", t);
      throw new ConstraintViolationException(violations);
    }
  }
}
