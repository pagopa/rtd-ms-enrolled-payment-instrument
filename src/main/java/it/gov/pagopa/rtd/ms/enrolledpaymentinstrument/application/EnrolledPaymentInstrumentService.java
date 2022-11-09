package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.EnrollAckError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.PaymentInstrumentEnrolled;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollAckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.Date;

@Service
@Slf4j
@Validated
public class EnrolledPaymentInstrumentService {

  private final EnrolledPaymentInstrumentRepository repository;
  private final EnrollAckService enrollAckService;

  public EnrolledPaymentInstrumentService(EnrolledPaymentInstrumentRepository repository, EnrollAckService enrollAckService) {
    this.repository = repository;
    this.enrollAckService = enrollAckService;
  }

  public void handle(@Valid EnrollPaymentInstrumentCommand command) {
    final var hashPan = HashPan.create(command.getHashPan());
    final var sourceApp = SourceApp.valueOf(command.getSourceApp().toUpperCase());

    final var paymentInstrument = repository.findByHashPan(hashPan.getValue())
            .orElse(EnrolledPaymentInstrument.createUnEnrolledInstrument(hashPan, command.getIssuer(), command.getNetwork()));

    if (command.getOperation() == Operation.CREATE) {
      paymentInstrument.enableApp(sourceApp);
    } else if (command.getOperation() == Operation.DELETE) {
      paymentInstrument.disableApp(sourceApp);
    }

    if (paymentInstrument.shouldBeDeleted()) {
      repository.delete(paymentInstrument);
    } else {
      handleDomainEvents(paymentInstrument);
      repository.save(paymentInstrument);
    }
  }

  private void handlePaymentInstrumentEnrolled(PaymentInstrumentEnrolled event) {
    if (enrollAckService.confirmEnroll(event.getApplication(), event.getHashPan(), new Date())) {
      log.info("Enroll ack successfully confirmed");
    } else {
      throw new EnrollAckError("Failing during ack " + event.getHashPan().getValue() + " from " + event.getApplication());
    }
  }

  private void handleDomainEvents(EnrolledPaymentInstrument paymentInstrument) {
    paymentInstrument.domainEvents().forEach(event -> {
      if (event instanceof PaymentInstrumentEnrolled)
        handlePaymentInstrumentEnrolled((PaymentInstrumentEnrolled) event);
    });
    paymentInstrument.clearDomainEvents();
  }
}
