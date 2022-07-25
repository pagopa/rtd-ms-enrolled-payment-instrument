package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.SomethingService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

@Slf4j
@Configuration
class KafkaAdapter {
  @Bean
  Consumer<Message<String>> enrolledPaymentInstrumentConsumer(SomethingService somethingService) {
    return message -> {
      log.info("Received message {}", message);
      final var result = somethingService.processSomething("1", message.getPayload());
      log.info("Message processed {}", result);
    };
  }
}
