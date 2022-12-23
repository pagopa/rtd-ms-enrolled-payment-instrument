package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain;

public class PaymentInstrumentError extends RuntimeException {
  public PaymentInstrumentError(String it, Throwable cause) {
    super(it, cause);
  }
}
