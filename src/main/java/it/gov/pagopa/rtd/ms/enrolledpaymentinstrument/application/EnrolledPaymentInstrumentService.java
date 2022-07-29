package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.exception.WriteConflict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class EnrolledPaymentInstrumentService {

  private boolean delay = true;

  private final EnrolledPaymentInstrumentRepository repository;

  public EnrolledPaymentInstrumentService(EnrolledPaymentInstrumentRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public boolean handle(EnrollPaymentInstrumentCommand command) {
    try {
      final var hashPan = HashPan.create(command.getHashPan());
      final var sourceApp = SourceApp.valueOf(command.getSourceApp().toUpperCase());

      final var paymentInstrument = repository.findByHashPan(hashPan.getValue())
          .orElse(EnrolledPaymentInstrument.create(hashPan, sourceApp, command.getIssuer(),
              command.getNetwork()));

      simulateConcurrencyEnv();

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

      return true;
    } catch (WriteConflict writeConflict) {
      log.error("Concurrency conflict", writeConflict);
      throw writeConflict;
    } catch (Exception error) {
      log.error("Error during enroll payment instrument", error);
      return false;
    }
  }

  private void simulateConcurrencyEnv() throws InterruptedException {
    if (delay) {
      delay = false;
      log.info("Sleeping");
      Thread.sleep(4000);
    } else {
      delay = true;
      log.info("No sleep, restored delay");
    }
  }
}
