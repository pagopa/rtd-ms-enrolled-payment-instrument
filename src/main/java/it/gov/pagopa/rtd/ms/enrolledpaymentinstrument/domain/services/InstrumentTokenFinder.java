package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import io.vavr.control.Try;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.InstrumentTokenInfo;
import org.slf4j.Logger;

import java.util.List;

@FunctionalInterface
public interface InstrumentTokenFinder {
  Try<InstrumentTokenInfo> findInstrumentInfo(HashPan hashPan);

  static InstrumentTokenFinder fake(Logger log) {
    log.info("Using fake InstrumentTokenFinder client");
    return hashPan -> {
      log.info("Fake finding instrument info");
      return Try.success(new InstrumentTokenInfo(hashPan, "", List.of()));
    };
  }
}
