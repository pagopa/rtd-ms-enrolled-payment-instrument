package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.virtualenroll;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cloud.function.cloudevent.CloudEventMessageBuilder;
import org.springframework.messaging.Message;

import java.util.Date;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VirtualEnroll {

  public static final String TYPE = "EnrollCard";

  @JsonAlias("hpan")
  private String hashPan;

  @JsonAlias("htoken")
  private String hashToken;

  private String par;

  private Date timestamp;

  public Message<VirtualEnroll> asCloudEvent() {
    return CloudEventMessageBuilder.withData(this).setType(TYPE).build();
  }
}
