package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event.EnrolledPaymentInstrumentEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
@Slf4j
public class EnrolledPaymentInstrumentRestControllerImpl implements
    EnrolledPaymentInstrumentRestController {

  private final EnrolledPaymentInstrumentService enrolledPaymentInstrumentService;
  private final KafkaTemplate<String, String> streamBridge;
  //private final StreamBridge streamBridge;

  @Autowired
  EnrolledPaymentInstrumentRestControllerImpl(
      EnrolledPaymentInstrumentService enrolledPaymentInstrumentService,
      KafkaTemplate<String, String> streamBridge
      //StreamBridge streamBridge
  ) {
    this.enrolledPaymentInstrumentService = enrolledPaymentInstrumentService;
    this.streamBridge = streamBridge;
  }

  @Override
  public void sendEnrolledPaymentEvent(EnrolledPaymentInstrumentEvent paymentInstrumentEvent) throws JsonProcessingException {
    log.info("Simulating send enrolled payment event: {}", paymentInstrumentEvent);
//    final var sent = streamBridge.send("enrolledPaymentInstrumentProducer-out-0",
//            paymentInstrumentEvent);
    final var sent = streamBridge.send("rtd-enrolled-events",
            new ObjectMapper().writeValueAsString(paymentInstrumentEvent));
    log.info("Event sent {}", sent);

//    enrolledPaymentInstrumentService.handle(new EnrollPaymentInstrumentCommand(
//        paymentInstrumentEvent.getHashPan(),
//        paymentInstrumentEvent.getApp(),
//        Operation.valueOf(paymentInstrumentEvent.getOperation().toUpperCase()),
//        paymentInstrumentEvent.getIssuer(),
//        paymentInstrumentEvent.getNetwork()
//    ));
  }
}
