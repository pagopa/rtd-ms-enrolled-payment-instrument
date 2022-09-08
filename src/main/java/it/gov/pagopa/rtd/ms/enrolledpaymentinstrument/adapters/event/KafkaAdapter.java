package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.exception.WriteConflict;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
class KafkaAdapter {

  private static final int BACKOFF_TIMEOUT_MS = 500;

  private final EnrolledPaymentInstrumentService paymentInstrumentService;

  public KafkaAdapter(EnrolledPaymentInstrumentService paymentInstrumentService) {
    this.paymentInstrumentService = paymentInstrumentService;
  }

  @SneakyThrows
  @Bean
  Consumer<Message<EnrolledPaymentInstrumentEvent>> enrolledPaymentInstrumentConsumer() {
    return message -> {
      final var partitionId = Optional.ofNullable(message.getHeaders().get(
          KafkaHeaders.PARTITION_ID)).orElse("");
      log.info("Received message {} on partition {}", message, partitionId);
      handleEvent(message.getPayload());
      log.info("Message successfully handled");
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

  @Bean
  ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> listenerCustomization() {
    return (container, dest, group) -> {
      final var errorHandler = new DefaultErrorHandler(
          new FixedBackOff(BACKOFF_TIMEOUT_MS, Long.MAX_VALUE)
      );
      errorHandler.setAckAfterHandle(false);
      errorHandler.defaultFalse();
      errorHandler.addRetryableExceptions(WriteConflict.class);

      container.getContainerProperties().setAckMode(AckMode.RECORD);
      container.setCommonErrorHandler(errorHandler);
    };
  }
}
