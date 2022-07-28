package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event.EnrolledPaymentInstrumentEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
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

  private final EnrolledPaymentInstrumentService enrolledPaymentInstrumentService;

  @Autowired
  EnrolledPaymentInstrumentRestControllerImpl(
      EnrolledPaymentInstrumentService enrolledPaymentInstrumentService,
      StreamBridge streamBridge
  ) {
    this.enrolledPaymentInstrumentService = enrolledPaymentInstrumentService;
  }

  @Override
  public void sendEnrolledPaymentEvent(EnrolledPaymentInstrumentEvent paymentInstrumentEvent) {
    log.info("Simulating send enrolled payment event: {}", paymentInstrumentEvent);
    enrolledPaymentInstrumentService.handle(new EnrollPaymentInstrumentCommand(
        paymentInstrumentEvent.getHashPan(),
        paymentInstrumentEvent.getApp(),
        Operation.valueOf(paymentInstrumentEvent.getOperation().toUpperCase()),
        paymentInstrumentEvent.getIssuer(),
        paymentInstrumentEvent.getNetwork()
    ));
  }
}
