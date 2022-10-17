package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class ApplicationEnrollEvent {

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
