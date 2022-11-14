package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("")
@Validated
public interface TestRestController {

  @PutMapping(value = "/tkm-update")
  @ResponseStatus(HttpStatus.OK)
  void sendTkmCardChangedEvent(@RequestBody TokenManagerCardChanged event);

  @GetMapping("/payment-instrument/{instrumentId}")
  EnrolledPaymentInstrument getPaymentInstrument(@PathVariable("instrumentId") String instrumentId);

  @DeleteMapping("/payment-instrument/{instrumentId}")
  @ResponseStatus(HttpStatus.OK)
  void deletePaymentInstrument(@PathVariable("instrumentId") String instrumentId);

}
