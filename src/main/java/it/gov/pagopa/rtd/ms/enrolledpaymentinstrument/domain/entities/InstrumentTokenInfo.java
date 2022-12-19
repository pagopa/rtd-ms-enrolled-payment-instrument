package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

@Data
@Getter
public final class InstrumentTokenInfo {
  private final HashPan hashPan;
  private final String par;
  private final List<HashPan> hashTokens;

  public Optional<String> getPar() {
    return Optional.ofNullable(this.par);
  }
}
