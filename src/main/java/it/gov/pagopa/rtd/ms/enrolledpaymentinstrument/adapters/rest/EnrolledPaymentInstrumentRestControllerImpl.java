package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event.EnrolledPaymentInstrumentEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ResponseBody
@Slf4j
public class EnrolledPaymentInstrumentRestControllerImpl implements
    EnrolledPaymentInstrumentRestController {

  private final EnrolledPaymentInstrumentService enrolledPaymentInstrumentService;
  private final StreamBridge streamBridge;

  @Autowired
  EnrolledPaymentInstrumentRestControllerImpl(
      EnrolledPaymentInstrumentService enrolledPaymentInstrumentService,
      StreamBridge streamBridge
  ) {
    this.enrolledPaymentInstrumentService = enrolledPaymentInstrumentService;
    this.streamBridge = streamBridge;
  }

  @Override
  public void sendEnrolledPaymentEvent(EnrolledPaymentInstrumentEvent paymentInstrumentEvent) {
    log.info("Simulating send enrolled payment event: {}", paymentInstrumentEvent);
    final var sent = streamBridge.send(
        "enrolledPaymentInstrumentProducer-out-0",
        MessageBuilder.withPayload(paymentInstrumentEvent)
            .setHeader("partitionKey",  paymentInstrumentEvent.getHashPan())
            .build()
      );
    log.info("Event sent {}", sent);
  }

  @Override
  public void sendEnrolledPaymentEventDirect(EnrolledPaymentInstrumentEvent paymentInstrumentEvent) {
    log.info("Simulating send enrolled payment event: {}", paymentInstrumentEvent);
    enrolledPaymentInstrumentService.handle(new EnrollPaymentInstrumentCommand(
        paymentInstrumentEvent.getHashPan(),
        paymentInstrumentEvent.getApp(),
        Operation.valueOf(paymentInstrumentEvent.getOperation().toUpperCase()),
        paymentInstrumentEvent.getIssuer(),
        paymentInstrumentEvent.getNetwork()
    ));
  }
}
