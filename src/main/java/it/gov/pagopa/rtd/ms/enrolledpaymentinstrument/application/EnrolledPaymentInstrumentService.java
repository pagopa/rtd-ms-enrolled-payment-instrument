package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class EnrolledPaymentInstrumentService {

  private boolean delay = true;

  @Autowired
  private EnrolledPaymentInstrumentRepository repository;

  @Transactional
  public boolean handle(EnrollPaymentInstrumentCommand command) {
    try {
      final var hashPan = HashPan.create(command.getHashPan());
      final var sourceApp = SourceApp.valueOf(command.getSourceApp().toUpperCase());

      final var paymentInstrument = repository.findByHashPan(hashPan.getValue())
          .orElse(EnrolledPaymentInstrument.create(hashPan, sourceApp, command.getIssuer(), command.getNetwork()));

      if (delay) {
        delay = false;
        log.info("Sleeping");
        Thread.sleep(4000);
      } else {
        delay = true;
        log.info("No sleep, restored delay");
      }

      if (command.isEnabled()) {
        paymentInstrument.enableApp(sourceApp);
      } else {
        paymentInstrument.disableApp(sourceApp);
      }

      repository.save(paymentInstrument);

      return true;
    } catch (Exception error) {
      log.error("Error during enroll payment instrument", error);
      return false;
    }
  }
}
