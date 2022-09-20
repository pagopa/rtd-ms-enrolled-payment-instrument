package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import java.util.function.Consumer;

@Slf4j
@Configuration
class KafkaAdapter {

  private final EnrolledPaymentInstrumentService paymentInstrumentService;

  public KafkaAdapter(EnrolledPaymentInstrumentService paymentInstrumentService) {
    this.paymentInstrumentService = paymentInstrumentService;
  }

  @SneakyThrows
  @Bean
  Consumer<Message<EnrolledPaymentInstrumentEvent>> enrolledPaymentInstrumentConsumer(
          Validator validator
  ) {
    return message -> {
      log.info("Received message {}", message);
      final var payload = message.getPayload();
      final var violations = validator.validate(payload);
      if (violations.isEmpty()) {
        handleEvent(payload);
        log.info("Message successfully handled");
      } else {
        log.error("Malformed event {}", payload);
        throw new ConstraintViolationException(violations);
      }
    };
  }

  private void handleEvent(EnrolledPaymentInstrumentEvent event) {
    paymentInstrumentService.handle(new EnrollPaymentInstrumentCommand(
        event.getHashPan(),
        event.getApp(),
        Operation.valueOf(event.getOperation().toUpperCase()),
        event.getIssuer(),
        event.getNetwork()
    ));
  }
}
