package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event.EnrolledPaymentInstrumentEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
@Slf4j
public class EnrolledPaymentInstrumentRestControllerImpl implements
    EnrolledPaymentInstrumentRestController {

  private final StreamBridge streamBridge;
  private final EnrolledPaymentInstrumentService enrolledPaymentInstrumentService;

  @Autowired
  EnrolledPaymentInstrumentRestControllerImpl(
      EnrolledPaymentInstrumentService enrolledPaymentInstrumentService,
      StreamBridge streamBridge
  ) {
    this.enrolledPaymentInstrumentService = enrolledPaymentInstrumentService;
    this.streamBridge = streamBridge;
  }

  private boolean byKafka = true;

  @Override
  public void sendEnrolledPaymentEvent(EnrolledPaymentInstrumentEvent paymentInstrumentEvent) {
    log.info("Simulating send enrolled payment event: {}", paymentInstrumentEvent);
    if (byKafka) {
      byKafka = false;
      final var sent = streamBridge.send("enrolledPaymentInstrumentProducer-out-0",
          paymentInstrumentEvent);
      log.info("Event sent {}", sent);
    } else {
      byKafka = true;
      enrolledPaymentInstrumentService.handle(new EnrollPaymentInstrumentCommand(
          paymentInstrumentEvent.getHashPan(),
          paymentInstrumentEvent.getApp(),
          paymentInstrumentEvent.isEnabled(),
          paymentInstrumentEvent.getIssuer(),
          paymentInstrumentEvent.getNetwork()
      ));
      log.info("aaa");
    }
  }
}
