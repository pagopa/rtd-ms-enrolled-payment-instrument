package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.VirtualEnrollService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.virtualenroll.VirtualEnroll;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationEnrollEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerWalletChanged;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@ResponseBody
@Slf4j
public class KafkaRestControllerImpl implements
        KafkaRestController {

  private static final String RTD_PRODUCER_BINDING = "rtdEnrolledPimProducer-out-0";
  private static final String TKM_BULK_CARD_BINDING = "tkmWalletEventConsumer-in-0";

  private final StreamBridge streamBridge;
  private final VirtualEnrollService virtualEnrollService;

  @Autowired
  KafkaRestControllerImpl(
          StreamBridge streamBridge,
          VirtualEnrollService virtualEnrollService
  ) {
    this.streamBridge = streamBridge;
    this.virtualEnrollService = virtualEnrollService;
  }

  @Override
  public void sendEnrolledPaymentEvent(ApplicationEnrollEvent paymentInstrumentEvent) {
    log.info("Simulating send enrolled payment event: {}", paymentInstrumentEvent);
    final var sent = streamBridge.send(
            RTD_PRODUCER_BINDING,
            MessageBuilder.withPayload(paymentInstrumentEvent)
                    .setHeader("partitionKey", paymentInstrumentEvent.getHashPan())
                    .build()
    );
    log.info("Event sent {}", sent);
  }

  @Override
  public void sendTkmUpdateEvent(TokenManagerWalletChanged event) {
    log.info("Sending tkm event {}", event);
    final var sent = streamBridge.send(
            TKM_BULK_CARD_BINDING,
            MessageBuilder.withPayload(event).setHeader("partitionKey", "0").build()
    );
    log.info("Tkm event sent {}", sent);
  }

  @Override
  public void sendTkmCardChangedEvent(TokenManagerCardChanged event) {
    log.info("Sending TokenManagerCardChanged: {}", event);
    final var sent = streamBridge.send(
            RTD_PRODUCER_BINDING,
            MessageBuilder.withPayload(event)
                    .setHeader("partitionKey", event.getHashPan())
                    .build()
    );
    log.info("TokenManagerCardChanged sent {}", sent);
  }

  @Override
  public void sendVirtualEnrollToApp(VirtualEnroll enroll) {
    log.info("Sending virtual enroll event");
    final var sent = virtualEnrollService.enroll(
            HashPan.create(enroll.getHashPan()),
            Optional.ofNullable(enroll.getHashToken()).map(HashPan::create).orElse(null),
            enroll.getPar()
    );
    log.info("Virtual enroll event sent {}", sent);
  }

}
