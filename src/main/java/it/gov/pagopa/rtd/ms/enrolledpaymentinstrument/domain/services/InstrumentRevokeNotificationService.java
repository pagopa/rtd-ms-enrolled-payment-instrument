package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;

public interface InstrumentRevokeNotificationService {
  boolean notifyRevoke(String taxCode, HashPan hashPan);
}
