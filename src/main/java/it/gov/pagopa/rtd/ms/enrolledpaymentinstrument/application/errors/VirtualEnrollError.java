package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;

public class VirtualEnrollError extends RuntimeException {
  public VirtualEnrollError() {
    super("Failed to virtual enroll");
  }

  public VirtualEnrollError(HashPan hashPan) {
    super("Failed to virtual enroll " + hashPan.getValue());
  }
}
