package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.EnrolledPaymentInstrumentEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest.resource.EnrolledPaymentInstrumentDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@ResponseBody
@Slf4j
public class EnrolledPaymentInstrumentRestControllerImpl implements
    EnrolledPaymentInstrumentRestController {

  private final StreamBridge streamBridge;
  private final EnrolledPaymentInstrumentRepository repository;

  @Autowired
  EnrolledPaymentInstrumentRestControllerImpl(
      StreamBridge streamBridge,
      EnrolledPaymentInstrumentRepository repository
  ) {
    this.streamBridge = streamBridge;
    this.repository = repository;
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
  public EnrolledPaymentInstrumentDto getEnrolledPaymentInstrument(String hashPan) {
    return repository.findByHashPan(hashPan).map(paymentInstrument ->
            EnrolledPaymentInstrumentDto.builder()
                    .hashPan(paymentInstrument.getHashPan().getValue())
                    .app(paymentInstrument.getEnabledApps().stream().map(Enum::name).collect(Collectors.toSet()))
                    .issuer(paymentInstrument.getIssuer())
                    .network(paymentInstrument.getNetwork())
                    .build()
    ).orElseThrow(() -> new ResourceNotFoundException("no enrolled payment instrument found"));
  }

}
