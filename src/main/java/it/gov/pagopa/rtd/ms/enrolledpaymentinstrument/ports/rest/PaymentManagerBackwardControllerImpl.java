package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter.TokenManagerCardEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest.dto.RevokeCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;

@RestController
@Slf4j
public class PaymentManagerBackwardControllerImpl implements PaymentManagerBackwardController {

  private final TokenManagerCardEventPublisher cardEventPublisher;

  public PaymentManagerBackwardControllerImpl(TokenManagerCardEventPublisher cardEventPublisher) {
    this.cardEventPublisher = cardEventPublisher;
  }

  @Override
  public void revokeCard(@Valid RevokeCard revokeCard) {
    log.info("Deleting card from PM backward API");
    final var isSent = cardEventPublisher.sendTokenManagerCardChanged(
            new TokenManagerCardChanged(
                    revokeCard.getHashPan(),
                    revokeCard.getTaxCode(),
                    null,
                    Collections.emptyList(),
                    CardChangeType.REVOKE
            )
    );
    if (isSent) log.info("Delete card event sent");
    else log.error("Failed to send delete card event");
  }
}
