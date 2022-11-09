package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.EnrollAckError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.PaymentInstrumentEnrolled;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollAckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Date;

@Component
@Validated
@Slf4j
public class EnrolledPaymentInstrumentEventListener {

  private final EnrollAckService enrollAckService;

  public EnrolledPaymentInstrumentEventListener(EnrollAckService enrollAckService) {
    this.enrollAckService = enrollAckService;
  }

  @EventListener
  public void handlePaymentInstrumentEnrolled(PaymentInstrumentEnrolled event) {
    if (enrollAckService.confirmEnroll(event.getApplication(), event.getHashPan(), new Date())) {
      log.info("Enroll ack successfully confirmed");
    } else {
      throw new EnrollAckError("Failing during ack " + event.getHashPan().getValue() + " from " + event.getApplication());
    }
  }
}
