package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.EnrolledPaymentInstrumentEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TkmUpdateEvent;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping("")
@Validated
public interface KafkaRestController {


  @PutMapping(value = "/enroll")
  @ResponseStatus(HttpStatus.OK)
  void sendEnrolledPaymentEvent(@RequestBody EnrolledPaymentInstrumentEvent paymentInstrumentEvent)
      throws JsonProcessingException;

  @PutMapping(value = "/tkm-update")
  @ResponseStatus(HttpStatus.OK)
  void sendTkmUpdateEvent(@RequestBody TkmUpdateEvent event);

}
