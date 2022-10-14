package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.Date;

@Data
@Getter
@AllArgsConstructor
public class RevokeNotification {

  private final String fiscalCode;

  @JsonAlias("hpan")
  private final String hashPan;

  private final Date deactivationDate;
}
