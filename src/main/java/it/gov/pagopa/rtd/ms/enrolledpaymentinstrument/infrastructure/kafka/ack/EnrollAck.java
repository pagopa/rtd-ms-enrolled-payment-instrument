package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.ack;

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
public final class EnrollAck {

  public static final String TYPE = "EnrollAck";

  @JsonAlias("hpan")
  private String hashPan;

  @JsonAlias("timestamp")
  private Date timestamp;
}
