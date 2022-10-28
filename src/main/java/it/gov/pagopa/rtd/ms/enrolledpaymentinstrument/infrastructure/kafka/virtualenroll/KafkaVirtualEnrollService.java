package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.virtualenroll;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.VirtualEnrollService;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.Date;
import java.util.Optional;

public class KafkaVirtualEnrollService implements VirtualEnrollService {

  private final String producerBinding;
  private final StreamBridge streamBridge;

  public KafkaVirtualEnrollService(String producerBinding, StreamBridge streamBridge) {
    this.producerBinding = producerBinding;
    this.streamBridge = streamBridge;
  }

  @Override
  public boolean enroll(HashPan hashPan, String par) {
    return enroll(hashPan, null, par);
  }

  @Override
  public boolean enroll(HashPan hashPan, HashPan token, String par) {
    final var enroll = new VirtualEnroll(
            hashPan.getValue(),
            Optional.ofNullable(token).map(HashPan::getValue).orElse(null),
            par,
            new Date()
    ).asCloudEvent();
    return streamBridge.send(
            producerBinding,
            enroll
    );
  }

  @Override
  public boolean unEnroll(HashPan hashPan, HashPan token, String par) {
    return streamBridge.send(
            producerBinding,
            new VirtualRevoke(hashPan.getValue(), token.getValue(), par, new Date()).asCloudEvent()
    );
  }
}
