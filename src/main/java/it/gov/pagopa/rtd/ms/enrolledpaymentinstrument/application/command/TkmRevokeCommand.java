package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class TkmRevokeCommand {

  @NotNull
  @NotBlank
  private final String taxCode;

  @NotNull
  @NotBlank
  private final String hashPan;

  private final String par;


}
