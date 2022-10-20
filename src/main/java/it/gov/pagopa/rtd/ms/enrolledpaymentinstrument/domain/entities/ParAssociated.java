package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class ParAssociated {
  private final HashPan hashPan;
  private final String par;
}
