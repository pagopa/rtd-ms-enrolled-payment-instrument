package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;

import java.util.Date;

public interface EnrollAckService {
  boolean confirmEnroll(HashPan hashPan, Date enrollDate);
}
