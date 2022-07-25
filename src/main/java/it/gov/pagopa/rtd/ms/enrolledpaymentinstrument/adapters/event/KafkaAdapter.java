package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.App;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

@Slf4j
@Configuration
class KafkaAdapter {
  @Bean
  Consumer<Message<String>> enrolledPaymentInstrumentConsumer(
      EnrolledPaymentInstrumentService paymentInstrumentService) {
    return message -> {
      log.info("Received message {}", message);
      final var result = paymentInstrumentService.handle(
          new EnrollPaymentInstrumentCommand("123234", "id_pay")
      );
      log.info("Message processed {}", result);
    };
  }
}
