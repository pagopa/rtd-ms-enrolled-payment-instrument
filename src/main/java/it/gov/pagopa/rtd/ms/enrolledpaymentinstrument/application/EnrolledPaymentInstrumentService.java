package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EnrolledPaymentInstrumentService {

  @Autowired
  private EnrolledPaymentInstrumentRepository repository;

  public boolean handle(EnrollPaymentInstrumentCommand command) {
    try {
      final var hashPan = HashPan.create(command.getHashPan());
      final var sourceApp = SourceApp.valueOf(command.getSourceApp().toUpperCase());

      final var paymentInstrument = repository.findById(hashPan.getValue())
          .orElse(EnrolledPaymentInstrument.create(hashPan, sourceApp));

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
