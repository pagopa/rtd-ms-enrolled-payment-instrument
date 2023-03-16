package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.EnrollAckError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.ExportError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.PaymentInstrumentEnrolled;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.PaymentInstrumentExported;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Date;

@Component
@Validated
@Slf4j
public class EnrolledPaymentInstrumentEventListener {

  private final EnrollNotifyService enrollNotifyService;

  public EnrolledPaymentInstrumentEventListener(EnrollNotifyService enrollNotifyService) {
    this.enrollNotifyService = enrollNotifyService;
  }

  @EventListener
  public void handlePaymentInstrumentEnrolled(PaymentInstrumentEnrolled event) {
    if (enrollNotifyService.confirmEnroll(event.getApplication(), event.getHashPan(), new Date())) {
      log.info("Enroll ack successfully confirmed");
    } else {
      throw new EnrollAckError("Failing during ack " + event.getHashPan().getValue() + " from " + event.getApplication());
    }
  }

  @EventListener
  public void handlePaymentInstrumentExported(PaymentInstrumentExported event) {
    if (enrollNotifyService.confirmExport(event.getHashPan(), new Date())) {
      log.info("Notified payment instrument exported {}", event);
    } else {
      throw new ExportError("Failed to notify export for " + event.getHashPan().getValue());
    }
  }
}