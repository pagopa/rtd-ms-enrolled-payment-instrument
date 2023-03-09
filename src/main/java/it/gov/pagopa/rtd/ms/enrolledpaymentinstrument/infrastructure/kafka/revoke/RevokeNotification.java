package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.revoke;

 import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RevokeNotification {

  public static final String TYPE = "RevokeCard";

  private String fiscalCode;

  @JsonProperty("hpan")
  private String hashPan;

  private Date timestamp;

  private Set<SourceApp> applications;
}
