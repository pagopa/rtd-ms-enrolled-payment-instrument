package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import java.util.concurrent.Future;

public interface EnrolledPaymentInstrumentRepository {
  Future<String> save(EnrolledPaymentInstrument enrolledPaymentInstrument);

  // others methods findBy...
}