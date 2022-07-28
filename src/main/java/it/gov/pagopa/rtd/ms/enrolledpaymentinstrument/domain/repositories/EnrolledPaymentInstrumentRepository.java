package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import java.util.Optional;

public interface EnrolledPaymentInstrumentRepository {
  Optional<EnrolledPaymentInstrument> findByHashPan(String id);

  String save(EnrolledPaymentInstrument enrolledPaymentInstrument);

  boolean delete(EnrolledPaymentInstrument enrolledPaymentInstrument);
}
