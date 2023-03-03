package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.PaymentInstrumentExported;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ResponseBody
@Slf4j
public class TestRestControllerImpl implements TestRestController {

  private static final String RTD_PRODUCER_BINDING = "rtdEnrolledPimProducer-out-0";

  private final StreamBridge streamBridge;
  private final EnrolledPaymentInstrumentRepository repository;

  @Autowired
  TestRestControllerImpl(
          StreamBridge streamBridge,
          EnrolledPaymentInstrumentRepository repository
  ) {
    this.streamBridge = streamBridge;
    this.repository = repository;
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
  public void sendExportEvent(PaymentInstrumentExported event) {
    log.info("Sending PaymentInstrumentExported: {}", event);
    final var sent = streamBridge.send(
            RTD_PRODUCER_BINDING,
            MessageBuilder.withPayload(event)
                    .setHeader("partitionKey", event.getPaymentInstrumentId())
                    .build()
    );
    log.info("PaymentInstrumentExported sent {}", sent);
  }

  @Override
  public EnrolledPaymentInstrument getPaymentInstrument(String instrumentId) {
    return repository.findByHashPan(instrumentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @Override
  public void deletePaymentInstrument(String instrumentId) {
    repository.findByHashPan(instrumentId).ifPresent(repository::delete);
  }

}
