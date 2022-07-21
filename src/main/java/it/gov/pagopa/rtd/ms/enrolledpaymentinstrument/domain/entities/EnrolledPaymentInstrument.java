package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

// Like data class
@Data
@AllArgsConstructor
public class EnrolledPaymentInstrument {
  private String something1;
  private final String something2;

  public void doSomething(String something) {
    this.something1 = something;
  }
}