package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest.dto.RevokeCard;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/pm-backward")
public interface PaymentManagerBackwardController {

  @DeleteMapping()
  void revokeCard(RevokeCard revokeCard);
}
