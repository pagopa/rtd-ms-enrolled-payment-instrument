package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.exception;

/**
 * Custom exception which describe write conflict due to a concurrency update of same
 * row / document.
 */
public class WriteConflict extends RuntimeException {

  public WriteConflict() {
  }

  public WriteConflict(String message) {
    super(message);
  }

  public WriteConflict(String message, Throwable cause) {
    super(message, cause);
  }

  public WriteConflict(Throwable cause) {
    super(cause);
  }

  public WriteConflict(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
