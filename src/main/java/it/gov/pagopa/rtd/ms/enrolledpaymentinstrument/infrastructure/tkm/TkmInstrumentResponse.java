package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.tkm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
class TkmInstrumentResponse {

  @JsonProperty("hpan")
  String hashPan;
  @JsonProperty("par")
  String par;
  @JsonProperty("htokens")
  List<String> hashTokens;
}
