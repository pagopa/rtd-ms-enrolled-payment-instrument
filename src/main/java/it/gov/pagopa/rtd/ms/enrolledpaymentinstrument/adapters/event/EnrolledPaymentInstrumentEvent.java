package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class EnrolledPaymentInstrumentEvent {

  @NotNull
  @NotBlank
  private String hashPan;

  @NotNull
  @NotBlank
  private String app;

  @NotNull
  @NotBlank
  private String operation;

  private String issuer;

  private String network;
}
