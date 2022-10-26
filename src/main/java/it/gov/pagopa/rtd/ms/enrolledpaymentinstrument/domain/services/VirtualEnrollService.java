package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;

public interface VirtualEnrollService {
  boolean enroll(HashPan hashPan, String par);
}
