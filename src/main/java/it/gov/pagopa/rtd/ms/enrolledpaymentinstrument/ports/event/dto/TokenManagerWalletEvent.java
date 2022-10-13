package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * This event is sent by tkm microservice, basically represent a user wallet with cards and token associated.
 * This message will be splitter to avoid bulk operations over db and
 * provide better horizontal scalability using Splitter EIP
 */
@Data
public final class TokenManagerWalletEvent {

  private final String taxCode;
  private final Date timestamp;
  private final List<CardItem> cards;

  @Data
  public static class CardItem {
    private final String hpan;
    private final String par;
    private final List<HashTokenItem> htokens;
  }

  @Data
  public static class HashTokenItem {
    private final String htoken;
    private final HashTokenAction haction;
  }
}
