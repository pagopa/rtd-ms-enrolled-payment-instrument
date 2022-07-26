package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrollPaymentInstrumentCommand {

  @NotNull
  @NotBlank
  private final String hashPan;

  @NotNull
  @NotBlank
  private final String sourceApp;

  @NotNull
  private final boolean enabled;

  private final String issuer;
  private final String network;
}