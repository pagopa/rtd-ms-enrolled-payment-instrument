package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.ack;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollAckService;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.Date;

public class KafkaEnrollAckService implements EnrollAckService {

  private final StreamBridge streamBridge;
  private final String binding;

  public KafkaEnrollAckService(StreamBridge streamBridge, String binding) {
    this.streamBridge = streamBridge;
    this.binding = binding;
  }

  @Override
  public boolean confirmEnroll(HashPan hashPan, Date enrollDate) {
    final var payload = new EnrollAck(hashPan.getValue(), enrollDate);
    return streamBridge.send(binding, CloudEvent.<EnrollAck>builder()
            .withType(EnrollAck.TYPE)
            .withData(payload)
            .build()
    );
  }
}
