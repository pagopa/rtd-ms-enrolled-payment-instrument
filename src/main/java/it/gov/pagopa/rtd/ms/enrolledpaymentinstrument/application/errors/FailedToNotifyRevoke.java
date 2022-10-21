package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors;

public class FailedToNotifyRevoke extends RuntimeException {

  public FailedToNotifyRevoke() {
    super("Failed to notify revoke");
  }
}
