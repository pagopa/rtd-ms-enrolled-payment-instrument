package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.EnrolledPaymentInstrumentEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TkmUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
@Slf4j
public class KafkaRestControllerImpl implements
        KafkaRestController {

  private final StreamBridge streamBridge;

  @Autowired
  KafkaRestControllerImpl(
      StreamBridge streamBridge
  ) {
    this.streamBridge = streamBridge;
  }

  @Override
  public void sendEnrolledPaymentEvent(EnrolledPaymentInstrumentEvent paymentInstrumentEvent) {
    log.info("Simulating send enrolled payment event: {}", paymentInstrumentEvent);
    final var sent = streamBridge.send(
        "enrolledPaymentInstrumentProducer-out-0",
        MessageBuilder.withPayload(paymentInstrumentEvent)
            .setHeader("partitionKey",  paymentInstrumentEvent.getHashPan())
            .build()
      );
    log.info("Event sent {}", sent);
  }

  @Override
  public void sendTkmUpdateEvent(TkmUpdateEvent event) {
    log.info("Sending tkm event {}", event);
    final var sent = streamBridge.send(
            "tkmTokenUpdateConsumer-in-0",
            MessageBuilder.withPayload(event).build()
    );
    log.info("Tkm event sent {}", sent);
  }


}
