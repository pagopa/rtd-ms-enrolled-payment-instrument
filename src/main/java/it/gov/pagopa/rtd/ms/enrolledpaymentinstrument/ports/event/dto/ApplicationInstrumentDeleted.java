package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import lombok.Data;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Getter
public final class ApplicationInstrumentDeleted {

  public static final String TYPE = "ApplicationInstrumentDeleted";

  @NotNull
  @NotBlank
  private final String hashPan;

  private final boolean allowTokenized;

  @NotNull
  @NotBlank
  private final String application;
}
