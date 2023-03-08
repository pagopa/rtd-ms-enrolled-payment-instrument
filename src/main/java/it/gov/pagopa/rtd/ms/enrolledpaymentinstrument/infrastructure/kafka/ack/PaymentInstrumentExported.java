package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.ack;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class PaymentInstrumentExported {

  public static final String TYPE = "PaymentInstrumentExported";

  @JsonProperty("hpan")
  private String hashPan;

  private Date timestamp;
}
