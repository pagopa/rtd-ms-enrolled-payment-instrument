package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.App;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class EnrolledPaymentInstrumentService {

  @Autowired
  private EnrolledPaymentInstrumentRepository repository;

  public boolean handle(EnrollPaymentInstrumentCommand command) {
    try {
      final var hashPan = HashPan.create(command.getHashPan());
      final var sourceApp = App.valueOf(command.getSourceApp().toUpperCase());

      final var paymentInstrument = repository.findById(hashPan.getValue())
          .orElse(EnrolledPaymentInstrument.create(hashPan, sourceApp));

      paymentInstrument.enrollFrom(sourceApp);

      repository.save(paymentInstrument);

      return true;
    } catch (Exception error) {
      error.printStackTrace();
      log.error("Error during enroll payment instrument", error);
      return false;
    }
  }
}
