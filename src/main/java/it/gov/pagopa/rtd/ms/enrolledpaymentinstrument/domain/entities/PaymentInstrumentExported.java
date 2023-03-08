package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEvent;
import lombok.Data;

@Data
public class PaymentInstrumentExported implements DomainEvent {
  private final HashPan hashPan;
}
