package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class PaymentInstrumentExported {

  public static final String TYPE = "confirmExport";

  @NotNull
  @NotBlank
  private String paymentInstrumentId;
}
