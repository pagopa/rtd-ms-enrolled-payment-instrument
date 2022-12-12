package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.tkm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
class TkmInstrumentResponse {

  @JsonProperty("hpan")
  final String hashPan;
  @JsonProperty("par")
  final String par;
  @JsonProperty("htokens")
  final List<String> hashTokens;
}
