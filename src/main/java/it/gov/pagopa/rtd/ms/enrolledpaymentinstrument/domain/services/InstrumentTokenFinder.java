package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import io.vavr.control.Try;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.InstrumentTokenInfo;

@FunctionalInterface
public interface InstrumentTokenFinder {
  Try<InstrumentTokenInfo> findInstrumentInfo(HashPan hashPan);
}
