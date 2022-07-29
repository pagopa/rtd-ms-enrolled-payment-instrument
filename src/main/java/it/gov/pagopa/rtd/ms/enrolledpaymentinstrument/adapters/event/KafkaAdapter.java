package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.exception.WriteConflict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
class KafkaAdapter {

  private final EnrolledPaymentInstrumentService paymentInstrumentService;

  public KafkaAdapter(EnrolledPaymentInstrumentService paymentInstrumentService) {
    this.paymentInstrumentService = paymentInstrumentService;
  }

  @KafkaListener(topics = "${spring.kafka.consumer.topic}")
  void enrolledPaymentInstrumentConsumer(@Payload EnrolledPaymentInstrumentEvent payload) {
    try {
      log.info("Received enroll event {}", payload);
      handleEvent(payload);
    } catch (WriteConflict conflict) {
      log.error("Write conflict, now retry");
      throw conflict;
    } catch (Throwable t) {
      log.error("Unexpected error, ignoring event", t);
    }
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

  @Bean
  ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
      final ConsumerFactory<String, String> consumerFactory) {

    final var errorHandler = new DefaultErrorHandler(
        new FixedBackOff(500, Long.MAX_VALUE)
    );
    errorHandler.setAckAfterHandle(false);
    errorHandler.defaultFalse();
    errorHandler.addRetryableExceptions(WriteConflict.class);

    final ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
    factory.setConsumerFactory(consumerFactory);
    factory.setCommonErrorHandler(errorHandler);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    return factory;
  }
}
