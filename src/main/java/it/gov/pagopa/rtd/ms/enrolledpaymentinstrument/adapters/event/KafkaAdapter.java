package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

@Slf4j
@Configuration
class KafkaAdapter {

  @Bean
  Consumer<Message<EnrolledPaymentInstrumentEvent>> enrolledPaymentInstrumentConsumer(
      EnrolledPaymentInstrumentService paymentInstrumentService) {
    return message -> {
      log.info("Received message {}", message);
      final var payload = message.getPayload();
      final var result = paymentInstrumentService.handle(
          new EnrollPaymentInstrumentCommand(
              payload.getHashPan(),
              payload.getApp(),
              Operation.valueOf(payload.getOperation().toUpperCase()),
              payload.getIssuer(),
              payload.getNetwork()
          )
      );
      log.info("Message processed {}", result);
    };
  }
}
