package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import javax.validation.Valid;
import java.util.Optional;
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
  Consumer<Message<EnrolledPaymentInstrumentEvent>> enrolledPaymentInstrumentConsumer() {
    return message -> {
      final var partitionId = Optional.ofNullable(message.getHeaders().get(
          KafkaHeaders.PARTITION_ID)).orElse("");
      log.info("Received message {} on partition {}", message, partitionId);
      handleEvent(message.getPayload());
      log.info("Message successfully handled");
    };
  }

  private void handleEvent(@Valid EnrolledPaymentInstrumentEvent event) {
    paymentInstrumentService.handle(new EnrollPaymentInstrumentCommand(
        event.getHashPan(),
        event.getApp(),
        Operation.valueOf(event.getOperation().toUpperCase()),
        event.getIssuer(),
        event.getNetwork()
    ));
  }
}
