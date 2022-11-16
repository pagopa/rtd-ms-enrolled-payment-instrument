package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;

import java.util.Set;

public interface VirtualEnrollService {
  boolean enroll(HashPan hashPan, String par, Set<SourceApp> applications);
  boolean enrollToken(HashPan hashPan, HashPan token, String par, Set<SourceApp> applications);
  boolean unEnrollToken(HashPan hashPan, HashPan token, String par, Set<SourceApp> applications);
}
