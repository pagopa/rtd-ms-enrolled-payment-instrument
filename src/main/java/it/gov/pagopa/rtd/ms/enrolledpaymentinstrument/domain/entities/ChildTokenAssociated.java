package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class ChildTokenAssociated {

  private final HashPan hashPan;
  private final HashPan childHashPan;
  private final String par;

}
