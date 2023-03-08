package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.ack;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollNotifyService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.CorrelationIdService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import java.util.Date;

public class KafkaEnrollNotifyService implements EnrollNotifyService {

  private final StreamBridge streamBridge;
  private final String binding;
  private final CorrelationIdService correlationIdService;

  public KafkaEnrollNotifyService(StreamBridge streamBridge, String binding, CorrelationIdService correlationIdService) {
    this.streamBridge = streamBridge;
    this.binding = binding;
    this.correlationIdService = correlationIdService;
  }

  @Override
  public boolean confirmEnroll(SourceApp app, HashPan hashPan, Date enrollDate) {
    final var payload = new EnrollAck(hashPan.getValue(), enrollDate, app);
    final var event = CloudEvent.<EnrollAck>builder()
            .withType(EnrollAck.TYPE)
            .withData(payload)
            .withCorrelationId(correlationIdService.popCorrelationId().orElse(null))
            .build();
    return streamBridge.send(binding, MessageBuilder.withPayload(event).setHeader("partitionKey", hashPan.getValue()).build());
  }

  @Override
  public boolean confirmExport(HashPan hashPan, Date at) {
    final var payload = new PaymentInstrumentExported(hashPan.getValue(), at);
    final var event = CloudEvent.builder()
            .withType(PaymentInstrumentExported.TYPE)
            .withData(payload)
            .build();
    return true;
    //return streamBridge.send(binding, MessageBuilder.withPayload(event).setHeader("partitionKey", hashPan.getValue()).build());
  }
}
