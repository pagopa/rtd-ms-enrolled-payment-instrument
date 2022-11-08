package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;

import java.util.Date;

public interface EnrollAckService {
  boolean confirmEnroll(SourceApp app, HashPan hashPan, Date enrollDate);
}
