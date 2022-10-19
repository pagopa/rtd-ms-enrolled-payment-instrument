package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokeCard {
  @NotNull
  @NotBlank
  private String taxCode;

  @NotNull
  @NotBlank
  private String hashPan;
}
