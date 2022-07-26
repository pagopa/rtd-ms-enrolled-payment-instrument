package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrolledPaymentInstrumentEvent {

  @NotNull
  @NotBlank
  private final String hashPan;

  @NotNull
  @NotBlank
  private final String app;

  @NotNull
  private final boolean enabled;

  private final String issuer;

  private final String network;
}
