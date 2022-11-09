package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.VirtualEnrollService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.virtualenroll.VirtualEnroll;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
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
    final var sent = virtualEnrollService.enrollToken(
            HashPan.create(enroll.getHashPan()),
            Optional.ofNullable(enroll.getHashToken()).map(HashPan::create).orElse(null),
            enroll.getPar(),
            enroll.getApplications()
    );
    log.info("Virtual enroll event sent {}", sent);
  }

}
