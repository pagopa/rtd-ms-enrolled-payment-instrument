package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RevokeNotification {

  private String fiscalCode;

  @JsonAlias("hpan")
  private String hashPan;

  private Date deactivationDate;
}
