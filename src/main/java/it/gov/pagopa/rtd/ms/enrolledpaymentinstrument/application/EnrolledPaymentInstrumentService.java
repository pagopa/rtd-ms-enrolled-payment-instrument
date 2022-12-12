package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentTokenFinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.function.Function;

@Service
@Slf4j
@Validated
public class EnrolledPaymentInstrumentService {

  private final EnrolledPaymentInstrumentRepository repository;
  private final InstrumentTokenFinder instrumentTokenFinder;
  private final DomainEventPublisher domainEventPublisher;

  public EnrolledPaymentInstrumentService(EnrolledPaymentInstrumentRepository repository, InstrumentTokenFinder instrumentTokenFinder, DomainEventPublisher domainEventPublisher) {
    this.repository = repository;
    this.instrumentTokenFinder = instrumentTokenFinder;
    this.domainEventPublisher = domainEventPublisher;
  }

  public void handle(@Valid EnrollPaymentInstrumentCommand command) {
    final var hashPan = HashPan.create(command.getHashPan());
    final var sourceApp = SourceApp.valueOf(command.getSourceApp().toUpperCase());

    final var paymentInstrument = repository.findByHashPan(hashPan.getValue())
            .orElse(EnrolledPaymentInstrument.createUnEnrolledInstrument(hashPan, command.getIssuer(), command.getNetwork()));

    if (command.getOperation() == Operation.CREATE) {
      paymentInstrument.enableApp(sourceApp);
      paymentInstrument.hydrateTokenAndParInfo(instrumentTokenFinder).getOrElseThrow(Function.identity());
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
}
