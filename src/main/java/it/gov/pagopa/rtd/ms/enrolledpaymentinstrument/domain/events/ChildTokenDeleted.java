package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import lombok.Data;
import lombok.Getter;

import java.util.Set;

@Data
@Getter
public class ChildTokenDeleted implements DomainEvent {

  private final HashPan hashPan;
  private final HashPan childHashPan;
  private final String par;
  private final Set<SourceApp> applications;

}