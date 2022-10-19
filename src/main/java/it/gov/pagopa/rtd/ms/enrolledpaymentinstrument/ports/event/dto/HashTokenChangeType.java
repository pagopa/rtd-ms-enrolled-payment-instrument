package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

/**
 * Action to execute over a single child hash token. Sent by TKM
 */
public enum HashTokenChangeType {
  INSERT_UPDATE,
  DELETE
}
