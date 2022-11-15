package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.virtualenroll;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
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
public class VirtualRevoke {

  public static final String TYPE = "RevokeToken";

  @JsonProperty("hpan")
  private String hashPan;

  @JsonProperty("htoken")
  private String hashToken;
  private String par;
  private Date timestamp;
  private Set<SourceApp> applications;

  public CloudEvent<VirtualRevoke> asCloudEvent() {
    return CloudEvent.<VirtualRevoke>builder()
            .withType(TYPE)
            .withData(this)
            .build();
  }
}
