package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;

public interface VirtualEnrollService {
  boolean enroll(HashPan hashPan, String par);
  boolean enroll(HashPan hashPan, HashPan token, String par);
  boolean unEnroll(HashPan hashPan, HashPan token, String par);
}
