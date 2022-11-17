package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.virtualenroll;

import com.fasterxml.jackson.annotation.JsonAlias;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VirtualRevoke {

  public static final String TYPE = "RevokeToken";

  @JsonAlias("hpan")
  private String hashPan;

  @JsonAlias("htoken")
  private String hashToken;
  private String par;
  private Date timestamp;

  public CloudEvent<VirtualRevoke> asCloudEvent() {
    return CloudEvent.<VirtualRevoke>builder()
            .withType(TYPE)
            .withData(this)
            .build();
  }
}
