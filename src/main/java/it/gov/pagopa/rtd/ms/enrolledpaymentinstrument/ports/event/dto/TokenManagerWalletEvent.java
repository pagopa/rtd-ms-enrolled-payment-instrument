package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public final class TokenManagerWalletEvent {

  private final String taxCode;
  private final Date timestamp;
  private final List<Card> cards;

  @Data
  public static class Card {
    private final String hpan;
    private final String par;
    private final List<String> htokens;
  }
}
