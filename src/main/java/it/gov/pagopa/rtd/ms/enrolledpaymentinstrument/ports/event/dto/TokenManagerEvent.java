package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenManagerEvent {

  @NotNull
  @NotBlank
  private String hashPan;

  private String par;

  private List<HashToken> hashTokens;

  private String operation;

  @Data
  static class HashToken {
    final String hashToken;
    final String operation;
  }
}
