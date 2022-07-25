package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrolledPaymentInstrumentEvent {
  private final String hashPan;
  private final String app;
  private final boolean enabled;
}
