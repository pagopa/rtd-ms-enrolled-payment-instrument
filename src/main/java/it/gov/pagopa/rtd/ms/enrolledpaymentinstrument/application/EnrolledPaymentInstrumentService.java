package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;

@Service
@Slf4j
@Validated
public class EnrolledPaymentInstrumentService {

  private final EnrolledPaymentInstrumentRepository repository;

  public EnrolledPaymentInstrumentService(EnrolledPaymentInstrumentRepository repository) {
    this.repository = repository;
  }

  public void handle(@Valid EnrollPaymentInstrumentCommand command) {
    final var hashPan = HashPan.create(command.getHashPan());
    final var sourceApp = SourceApp.valueOf(command.getSourceApp().toUpperCase());

    final var paymentInstrument = repository.findByHashPan(hashPan.getValue())
            .orElse(EnrolledPaymentInstrument.create(hashPan, sourceApp, command.getIssuer(),
                    command.getNetwork()));

    if (command.getOperation() == Operation.CREATE) {
      paymentInstrument.enableApp(sourceApp);
    } else if (command.getOperation() == Operation.DELETE){
      paymentInstrument.disableApp(sourceApp);
    }

    if (paymentInstrument.isShouldBeDeleted()) {
      repository.delete(paymentInstrument);
    } else {
      repository.save(paymentInstrument);
    }
  }
}
