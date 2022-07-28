package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import java.util.Optional;
import java.util.concurrent.Future;

public interface EnrolledPaymentInstrumentRepository {
  Optional<EnrolledPaymentInstrument> findByHashPan(String id);

  Future<String> save(EnrolledPaymentInstrument enrolledPaymentInstrument);

  boolean delete(EnrolledPaymentInstrument enrolledPaymentInstrument);
}
