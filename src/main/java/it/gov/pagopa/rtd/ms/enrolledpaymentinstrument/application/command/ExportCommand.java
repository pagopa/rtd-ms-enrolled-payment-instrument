package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ExportCommand {
  private final String hashPan;
  private final OffsetDateTime exportedAt;
}
