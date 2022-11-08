package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.revoke;

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

  public static final String TYPE = "RevokeCard";

  private String fiscalCode;

  @JsonAlias("hpan")
  private String hashPan;

  private Date timestamp;
}
