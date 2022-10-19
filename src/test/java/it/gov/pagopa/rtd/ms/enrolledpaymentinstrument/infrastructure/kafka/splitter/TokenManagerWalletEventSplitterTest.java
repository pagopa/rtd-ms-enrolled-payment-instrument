package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.HashTokenChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerWalletChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerWalletChanged.HashTokenItem;

class TokenManagerWalletEventSplitterTest {

  private TokenManagerWalletEventSplitter splitter;

  @BeforeEach
  void setUp() {
    splitter = new TokenManagerWalletEventSplitter();
  }

  @Test
  void whenWalletContainsMultipleCardThenTaxCodeIsCopiedToSplitEvents() {
    final var cards = IntStream.of(3).mapToObj(i ->
            new TokenManagerWalletChanged.CardItem(
                    TestUtils.generateRandomHashPan().getValue(),
                    "par",
                    CardChangeType.INSERT_UPDATE,
                    Collections.emptyList()
            )
    ).collect(Collectors.toList());

    final var walletEvent = new TokenManagerWalletChanged("taxCode", LocalDateTime.now(), cards);
    final var cardEvents = splitter.apply(walletEvent);

    assertThat(cardEvents)
            .isNotEmpty()
            .allMatch(it -> Objects.equals(it.getTaxCode(), "taxCode"));
  }

  @Test
  void whenWalletContainsMultipleCardThenEachCardIsAnEvent() {
    final var hashTokens = List.of(
            new HashTokenItem(TestUtils.generateRandomHashPanAsString(), HashTokenChangeType.UPDATE),
            new HashTokenItem(TestUtils.generateRandomHashPanAsString(), HashTokenChangeType.UPDATE),
            new HashTokenItem(TestUtils.generateRandomHashPanAsString(), HashTokenChangeType.DELETE)
    );
    final var cards = IntStream.of(3).mapToObj(i ->
            new TokenManagerWalletChanged.CardItem(
                    TestUtils.generateRandomHashPanAsString(),
                    "par",
                    CardChangeType.INSERT_UPDATE,
                    hashTokens
            )
    ).collect(Collectors.toList());

    final var walletEvent = new TokenManagerWalletChanged("taxCode", LocalDateTime.now(), cards);
    final var cardEvents = splitter.apply(walletEvent);

    // assert over hash tokens
    assertThat(cardEvents)
            .flatMap(TokenManagerCardChanged::getHashTokens)
            .isNotEmpty()
            .allMatch(it -> hashTokens.stream().anyMatch(that ->
                    it.getHashToken().equals(that.getHtoken()) && it.getChangeType() == that.getHaction()
            ));

    assertThat(cardEvents)
            .isNotEmpty()
            .allMatch(it -> cards.stream().anyMatch(that ->
                    that.getHpan().equals(it.getHashPan()) && that.getPar().equals(it.getPar()) &&
                            that.getAction() == it.getChangeType()
            ));
  }

  @Test
  void whenWalletCardsHaveNullTokensThenCardEventHaveNoTokens() {
    final var events = splitter.apply(
            new TokenManagerWalletChanged("taxCode", LocalDateTime.now(),
                    List.of(
                            new TokenManagerWalletChanged.CardItem(
                                    "hpan",
                                    "par",
                                    CardChangeType.REVOKE,
                                    null
                            )
                    )
            )
    );

    assertThat(events).isNotEmpty().allMatch(it -> it.getHashTokens().isEmpty());
  }

  @Test
  void whenWalletHasNullCardThenSplitIntoEmptyList() {
    final var cardEvents = splitter.apply(
            new TokenManagerWalletChanged("taxCode", LocalDateTime.now(), null)
    );
    assertThat(cardEvents).isEmpty();
  }
}