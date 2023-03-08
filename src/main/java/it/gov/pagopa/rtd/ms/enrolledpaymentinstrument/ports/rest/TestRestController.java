package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.PaymentInstrumentExported;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * A non-functional REST interface.
 * It's useful to make test from outside allowing to inspect the state of a payment instrument.
 */
@RequestMapping("")
@Validated
public interface TestRestController {

  @PutMapping(value = "/tkm-update")
  @ResponseStatus(HttpStatus.OK)
  void sendTkmCardChangedEvent(@RequestBody TokenManagerCardChanged event);

  @PutMapping(value = "/export-event")
  @ResponseStatus(HttpStatus.OK)
  void sendExportEvent(@RequestBody PaymentInstrumentExported event);

  @GetMapping("/payment-instrument/{instrumentId}")
  EnrolledPaymentInstrument getPaymentInstrument(@PathVariable("instrumentId") String instrumentId);

  @DeleteMapping("/payment-instrument/{instrumentId}")
  @ResponseStatus(HttpStatus.OK)
  void deletePaymentInstrument(@PathVariable("instrumentId") String instrumentId);

}
