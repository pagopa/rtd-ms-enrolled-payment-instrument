package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerWalletChanged;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TokenManagerWalletEventSplitter implements Function<TokenManagerWalletChanged, List<TokenManagerCardChanged>> {

  @Override
  public List<TokenManagerCardChanged> apply(TokenManagerWalletChanged tokenManagerWalletChanged) {
    return Optional.ofNullable(tokenManagerWalletChanged.getCards())
            .orElse(Collections.emptyList())
            .stream()
            .map(it -> TokenManagerCardChanged.builder()
                    .hashPan(it.getHpan())
                    .par(it.getPar())
                    .changeType(it.getAction())
                    .taxCode(tokenManagerWalletChanged.getTaxCode())
                    .hashTokens(buildHashTokenEvents(it))
                    .timestamp(tokenManagerWalletChanged.getTimestamp())
                    .build()
            )
            .collect(Collectors.toList());
  }

  private List<TokenManagerCardChanged.HashTokenEvent> buildHashTokenEvents(TokenManagerWalletChanged.CardItem card) {
    return Optional.ofNullable(card.getHtokens())
            .orElse(Collections.emptyList())
            .stream()
            .map(token -> new TokenManagerCardChanged.HashTokenEvent(token.getHtoken(), token.getHaction()))
            .collect(Collectors.toList());
  }
}
