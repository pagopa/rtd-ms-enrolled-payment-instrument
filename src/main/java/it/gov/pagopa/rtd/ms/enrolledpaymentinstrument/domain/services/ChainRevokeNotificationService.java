package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;

import java.util.List;
import java.util.Set;

/**
 * This class allows to define a chain of delegated revoke notification service. It's useful to notify
 * multiple concrete destination (e.g. publish to kafka queue or microservices through rest).
 * It allows to define a list of RevokeNotificationService which are called in a sequence manner, if one
 * of these fails the next one will not be called and notifyRevoke returns false. True are returned only
 * when all delegated revoke services returns true.
 */
public class ChainRevokeNotificationService implements InstrumentRevokeNotificationService {

  private final List<InstrumentRevokeNotificationService> notificationServices;

  public ChainRevokeNotificationService(List<InstrumentRevokeNotificationService> notificationServices) {
    this.notificationServices = notificationServices;
  }

  @Override
  public boolean notifyRevoke(Set<SourceApp> apps, String taxCode, HashPan hashPan) {
    return notificationServices.stream().allMatch(it -> it.notifyRevoke(apps, taxCode, hashPan));
  }
}
