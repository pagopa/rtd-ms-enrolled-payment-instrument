package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import lombok.Data;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Getter
public final class ApplicationInstrumentAdded {

  public static final String TYPE = "ApplicationInstrumentAdded";

  @NotNull
  @NotBlank
  private final String hashPan;

  private final boolean allowTokenized;

  @NotNull
  @NotBlank
  private final String application;
}
