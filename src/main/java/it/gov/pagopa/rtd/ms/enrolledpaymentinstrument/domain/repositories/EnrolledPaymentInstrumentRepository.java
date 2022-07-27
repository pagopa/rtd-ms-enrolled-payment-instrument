package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import java.util.Optional;
import java.util.concurrent.Future;

public interface EnrolledPaymentInstrumentRepository {
  Future<String> save(EnrolledPaymentInstrument enrolledPaymentInstrument);
  Optional<EnrolledPaymentInstrument> findByHashPan(String id);

  // others methods findBy...
}
