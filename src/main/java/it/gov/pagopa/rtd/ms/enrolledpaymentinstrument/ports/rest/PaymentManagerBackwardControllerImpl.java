package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter.TokenManagerCardEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
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

  @ExceptionHandler(InternalError.class)
  ResponseEntity<Object> handleInternalError() {
    return ResponseEntity.internalServerError().build();
  }

  @Override
  public void revokeCard(String hashPan, String taxCode) {
    log.info("Deleting card from PM backward API");
    final var isSent = cardEventPublisher.sendTokenManagerCardChanged(
            new TokenManagerCardChanged(
                    hashPan,
                    taxCode,
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
}
