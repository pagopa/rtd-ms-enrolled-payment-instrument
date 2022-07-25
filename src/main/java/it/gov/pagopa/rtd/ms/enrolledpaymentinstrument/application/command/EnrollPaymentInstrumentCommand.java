package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrollPaymentInstrumentCommand {
  private final String hashPan;
  private final String sourceApp;
}