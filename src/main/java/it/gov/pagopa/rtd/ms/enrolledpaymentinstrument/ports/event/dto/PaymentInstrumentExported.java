package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import lombok.Data;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Getter
public final class PaymentInstrumentExported {

  public static final String TYPE = "confirmExport";

  @NotNull
  @NotBlank
  private final String paymentInstrumentId;
}
