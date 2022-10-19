package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter.TokenManagerCardEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest.dto.RevokeCard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;

@RestController
@RequestMapping("/pm-backward")
@Slf4j
public class PaymentManagerBackwardControllerImpl implements PaymentManagerBackwardController {

  private final TokenManagerCardEventPublisher cardEventPublisher;

  public PaymentManagerBackwardControllerImpl(TokenManagerCardEventPublisher cardEventPublisher) {
    this.cardEventPublisher = cardEventPublisher;
  }

  @Override
  public void revokeCard(RevokeCard revokeCard) {
    log.info("Deleting card from PM backward API");
    final var isSent = cardEventPublisher.sendTokenManagerCardChanged(
            new TokenManagerCardChanged(
                    revokeCard.getHashPan(),
                    revokeCard.getTaxCode(),
                    null,
                    Collections.emptyList(),
                    LocalDateTime.now(),
                    CardChangeType.REVOKE
            )
    );
    if (isSent) log.info("Delete card event sent");
    else {
      log.error("Failed to send delete card event");
      throw new InternalError("Failed to send revoke card event");
    }
  }

  @ExceptionHandler(InternalError.class)
  ResponseEntity<Object> handleInternalError() {
    return ResponseEntity.internalServerError().build();
  }
}
