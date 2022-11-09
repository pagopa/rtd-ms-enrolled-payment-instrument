package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEvent;
import lombok.Data;
import lombok.Getter;

import java.util.Set;

@Data
@Getter
public class ParAssociated implements DomainEvent {
  private final HashPan hashPan;
  private final String par;
  private final Set<SourceApp> applications;
}