package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event.EnrolledPaymentInstrumentEvent;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("")
@Validated
public interface EnrolledPaymentInstrumentRestController {


  @PutMapping(value = "/send")
  @ResponseStatus(HttpStatus.OK)
  void sendEnrolledPaymentEvent(@RequestBody EnrolledPaymentInstrumentEvent paymentInstrumentEvent)
      throws JsonProcessingException;

  @PutMapping(value = "/send-direct")
  @ResponseStatus(HttpStatus.OK)
  void sendEnrolledPaymentEventDirect(@RequestBody EnrolledPaymentInstrumentEvent paymentInstrumentEvent);
}
