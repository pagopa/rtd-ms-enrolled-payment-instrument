package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors;

public class ExportError extends RuntimeException {
  public ExportError(String message) {
    super(message);
  }
}
