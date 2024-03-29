package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;

import java.util.Date;

public interface EnrollNotifyService {
  boolean confirmEnroll(SourceApp app, HashPan hashPan, Date enrollDate);
  boolean confirmExport(HashPan hashPan, Date at);
}
