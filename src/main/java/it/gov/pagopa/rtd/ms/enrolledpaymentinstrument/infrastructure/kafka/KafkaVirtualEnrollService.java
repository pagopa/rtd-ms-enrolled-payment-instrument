package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.VirtualEnrollService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;

import java.util.HashMap;

public class KafkaVirtualEnrollService implements VirtualEnrollService {

  private final String producerBinding;
  private final StreamBridge streamBridge;

  public KafkaVirtualEnrollService(String producerBinding, StreamBridge streamBridge) {
    this.producerBinding = producerBinding;
    this.streamBridge = streamBridge;
  }

  @Override
  public boolean enroll(HashPan hashPan, String par) {
    final var tmpPayload = new HashMap<String, Object>();
    tmpPayload.put("hashPan", hashPan.getValue());
    tmpPayload.put("par", par);
    return streamBridge.send(
            producerBinding,
            MessageBuilder.withPayload(tmpPayload).build()
    );
  }


}
