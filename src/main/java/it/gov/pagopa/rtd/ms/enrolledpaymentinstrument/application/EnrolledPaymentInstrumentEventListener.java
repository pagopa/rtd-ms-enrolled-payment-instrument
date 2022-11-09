package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.EnrollAckError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.VirtualEnrollError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.ChildTokenAssociated;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.ParAssociated;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.PaymentInstrumentEnrolled;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollAckService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.VirtualEnrollService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Date;
import java.util.Set;

@Component
@Validated
@Slf4j
public class EnrolledPaymentInstrumentEventListener {

  private final EnrollAckService enrollAckService;
  private final VirtualEnrollService virtualEnrollService;

  public EnrolledPaymentInstrumentEventListener(EnrollAckService enrollAckService, VirtualEnrollService virtualEnrollService) {
    this.enrollAckService = enrollAckService;
    this.virtualEnrollService = virtualEnrollService;
  }

  @EventListener
  public void handlePaymentInstrumentEnrolled(PaymentInstrumentEnrolled event) {
    if (enrollAckService.confirmEnroll(event.getApplication(), event.getHashPan(), new Date())) {
      log.info("Enroll ack successfully confirmed");
    } else {
      throw new EnrollAckError("Failing during ack " + event.getHashPan().getValue() + " from " + event.getApplication());
    }
  }

  @EventListener
  public void handleParAssociated(ParAssociated event) {
    log.info("Handling Par Associated Event, doing virtual enroll");
    performVirtualEnroll(event.getHashPan(), event.getPar(), event.getApplications());
  }

  @EventListener
  public void handleChildTokenAssociated(ChildTokenAssociated event) {
    log.info("Handling Child Token Associated Event, doing virtual enroll");
    performVirtualEnroll(event.getChildHashPan(), event.getPar(), event.getApplications());
  }

  private void performVirtualEnroll(HashPan hashPan, String par, Set<SourceApp> apps) {
    if (virtualEnrollService.enroll(hashPan, par, apps)) {
      log.info("Virtual enroll done");
    } else {
      log.error("Failed during virtual enroll");
      throw new VirtualEnrollError();
    }
  }
}
