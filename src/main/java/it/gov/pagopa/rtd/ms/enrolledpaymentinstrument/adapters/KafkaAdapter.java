package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.SomethingService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

@Slf4j
@Configuration
public class KafkaAdapter {
  @Bean
  public Consumer<Message<String>> enrolledPaymentInstrumentConsumer(SomethingService somethingService) {
    return message -> {
      log.info("Received message {}", message);
      final var result = somethingService.processSomething(message.getPayload());
      log.info("Message processed {}", result);
    };
  }
}
