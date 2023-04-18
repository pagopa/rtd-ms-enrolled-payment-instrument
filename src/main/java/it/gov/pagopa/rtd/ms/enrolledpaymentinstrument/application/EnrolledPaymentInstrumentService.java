package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.ExportCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentTokenFinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import java.util.Objects;

@Service
@Slf4j
@Validated
public class EnrolledPaymentInstrumentService {

  private final EnrolledPaymentInstrumentRepository repository;
  private final InstrumentTokenFinder instrumentTokenFinder;
  private final DomainEventPublisher domainEventPublisher;
  private final Boolean immediateConfirmExport;

  public EnrolledPaymentInstrumentService(
          EnrolledPaymentInstrumentRepository repository,
          InstrumentTokenFinder instrumentTokenFinder,
          DomainEventPublisher domainEventPublisher,
          @Value("${immediate-confirm-export}") Boolean immediateConfirmExport
  ) {
    this.repository = repository;
    this.instrumentTokenFinder = instrumentTokenFinder;
    this.domainEventPublisher = domainEventPublisher;
    this.immediateConfirmExport = immediateConfirmExport;
  }

  public void handle(@Valid EnrollPaymentInstrumentCommand command) {
    final var hashPan = HashPan.create(command.getHashPan());
    final var sourceApp = SourceApp.valueOf(command.getSourceApp().toUpperCase());

    final var paymentInstrument = repository.findByHashPan(hashPan.getValue())
            .orElse(EnrolledPaymentInstrument.create(hashPan, sourceApp, command.getIssuer(), command.getNetwork()));

    if (command.getOperation() == Operation.CREATE) {
      paymentInstrument.enableApp(sourceApp);
      paymentInstrument.hydrateTokenAndParInfo(instrumentTokenFinder)
              .peekLeft(error -> log.warn("Failed to GET token par info: {}", error.getMessage()));
      if (Objects.equals(true, immediateConfirmExport)) {
        paymentInstrument.markAsExported();
      }
    } else if (command.getOperation() == Operation.DELETE) {
      paymentInstrument.disableApp(sourceApp);
    }

    if (paymentInstrument.shouldBeDeleted()) {
      repository.delete(paymentInstrument);
    } else {
      domainEventPublisher.handle(paymentInstrument);
      repository.save(paymentInstrument);
    }
  }

  public void handle(@Valid ExportCommand command) {
    final var paymentInstrumentOrEmpty = repository.findByHashPan(command.getHashPan());
    if (paymentInstrumentOrEmpty.isPresent()) {
      final var paymentInstrument = paymentInstrumentOrEmpty.get();
      paymentInstrument.markAsExported();
      domainEventPublisher.handle(paymentInstrument);
      repository.save(paymentInstrument);
    } else {
      log.warn("Received export command for non existing payment instrument");
    }
  }
}
