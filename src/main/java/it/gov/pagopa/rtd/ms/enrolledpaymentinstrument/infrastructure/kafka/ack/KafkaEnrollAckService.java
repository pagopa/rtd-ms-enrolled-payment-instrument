package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.ack;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollAckService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.CorrelationIdService;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.Date;

public class KafkaEnrollAckService implements EnrollAckService {

  private final StreamBridge streamBridge;
  private final String binding;
  private final CorrelationIdService correlationIdService;

  public KafkaEnrollAckService(StreamBridge streamBridge, String binding, CorrelationIdService correlationIdService) {
    this.streamBridge = streamBridge;
    this.binding = binding;
    this.correlationIdService = correlationIdService;
  }

  @Override
  public boolean confirmEnroll(SourceApp app, HashPan hashPan, Date enrollDate) {
    final var payload = new EnrollAck(hashPan.getValue(), enrollDate, app);
    return streamBridge.send(binding, CloudEvent.<EnrollAck>builder()
            .withType(EnrollAck.TYPE)
            .withData(payload)
            .withCorrelationId(correlationIdService.popCorrelationId().orElse(null))
            .build()
    );
  }
}
