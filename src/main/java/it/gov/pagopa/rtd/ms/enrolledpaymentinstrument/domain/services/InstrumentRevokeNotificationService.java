package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;

import java.util.Set;

public interface InstrumentRevokeNotificationService {
  boolean notifyRevoke(Set<SourceApp> apps, String taxCode, HashPan hashPan);
}
