package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEvent;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public final class PaymentInstrumentEnrolled implements DomainEvent {

  private final HashPan hashPan;
  private final SourceApp application;

}
