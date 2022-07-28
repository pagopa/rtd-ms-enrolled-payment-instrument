package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import java.time.Duration;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
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
              payload.isEnabled(),
              payload.getIssuer(),
              payload.getNetwork()
          )
      );
      log.info("Message processed {}", result);
    };
  }
}
