package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class PaymentInstrumentExported {

  public static final String TYPE = "ConfirmExport";

  @NotNull
  @NotBlank
  private String paymentInstrumentId;
}
