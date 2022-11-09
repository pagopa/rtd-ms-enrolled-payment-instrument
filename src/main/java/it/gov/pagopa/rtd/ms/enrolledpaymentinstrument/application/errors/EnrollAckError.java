package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors;

public class EnrollAckError extends RuntimeException {
  public EnrollAckError(String message) {
    super(message);
  }
}
