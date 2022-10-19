package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * This event is sent by tkm microservice, basically represent a user wallet with cards and token associated.
 * This message will be splitter to avoid bulk operations over db and
 * provide better horizontal scalability using Splitter EIP
 */
@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class TokenManagerWalletChanged {

  private String taxCode;
  @JsonFormat(pattern =  "yyyy-MM-dd HH:mm:ss:SSSS")
  private LocalDateTime timestamp;
  private List<CardItem> cards;

  @Data
  public static class CardItem {
    private final String hpan;
    private final String par;
    private final CardChangeType action;
    private final List<HashTokenItem> htokens;
  }

  @Data
  public static class HashTokenItem {
    private final String htoken;
    private final HashTokenChangeType haction;
  }
}
