package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.EnrolledPaymentInstrumentEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest.resource.EnrolledPaymentInstrumentDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("")
@Validated
public interface EnrolledPaymentInstrumentRestController {


  @PutMapping(value = "/send")
  @ResponseStatus(HttpStatus.OK)
  void sendEnrolledPaymentEvent(@RequestBody EnrolledPaymentInstrumentEvent paymentInstrumentEvent)
      throws JsonProcessingException;


  @GetMapping("/{hashPan}")
  EnrolledPaymentInstrumentDto getEnrolledPaymentInstrument(@PathVariable String hashPan);
}
