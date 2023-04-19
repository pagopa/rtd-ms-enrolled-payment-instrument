package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
  private final Operation operation;

  private final String issuer;
  private final String network;

   public enum Operation {
    CREATE,
    DELETE
  }

  public EnrollPaymentInstrumentCommand(
          String hashPan,
          String sourceApp,
          Operation operation
  ) {
     this(hashPan, sourceApp, operation, null, null);
  }
}