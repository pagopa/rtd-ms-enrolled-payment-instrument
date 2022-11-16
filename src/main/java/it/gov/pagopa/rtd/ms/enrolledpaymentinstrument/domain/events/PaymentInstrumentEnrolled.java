package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class PaymentInstrumentEnrolled implements DomainEvent {

  private final HashPan hashPan;
  private final SourceApp application;

}
