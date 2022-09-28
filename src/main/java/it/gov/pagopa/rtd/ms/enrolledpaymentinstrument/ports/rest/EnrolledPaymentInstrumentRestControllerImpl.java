package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.EnrolledPaymentInstrumentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
@Slf4j
public class EnrolledPaymentInstrumentRestControllerImpl implements
    EnrolledPaymentInstrumentRestController {

  private final StreamBridge streamBridge;

  @Autowired
  EnrolledPaymentInstrumentRestControllerImpl(
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

}
