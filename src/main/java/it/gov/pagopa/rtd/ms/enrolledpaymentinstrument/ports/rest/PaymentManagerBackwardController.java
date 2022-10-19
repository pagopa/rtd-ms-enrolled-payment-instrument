package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest.dto.RevokeCard;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

@Validated
public interface PaymentManagerBackwardController {

  @DeleteMapping()
  void revokeCard(@Valid @RequestBody RevokeCard revokeCard);
}