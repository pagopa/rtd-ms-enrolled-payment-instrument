package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.function.Consumer;

@AllArgsConstructor
@Slf4j
public class ValidatedConsumer<T> implements Consumer<T> {

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
