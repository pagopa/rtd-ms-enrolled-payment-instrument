package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.exception;

/**
 * Custom exception which describe write conflict due to a concurrency update of same
 * row / document.
 */
public class WriteConflict extends RuntimeException {
  public WriteConflict(Throwable cause) {
    super(cause);
  }
}
