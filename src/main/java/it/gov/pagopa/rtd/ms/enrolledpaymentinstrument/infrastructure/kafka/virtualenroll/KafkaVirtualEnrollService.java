package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.virtualenroll;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.VirtualEnrollService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

public class KafkaVirtualEnrollService implements VirtualEnrollService {

  private final String producerBinding;
  private final StreamBridge streamBridge;

  public KafkaVirtualEnrollService(String producerBinding, StreamBridge streamBridge) {
    this.producerBinding = producerBinding;
    this.streamBridge = streamBridge;
  }

  @Override
  public boolean enroll(HashPan hashPan, String par, Set<SourceApp> applications) {
    return enrollToken(hashPan, null, par, applications);
  }

  @Override
  public boolean enrollToken(HashPan hashPan, HashPan token, String par, Set<SourceApp> applications) {
    final var optionalHashToken = Optional.ofNullable(token).map(HashPan::getValue).orElse(null);
    final var enroll = new VirtualEnroll(hashPan.getValue(), optionalHashToken, par, new Date(), applications).asCloudEvent();
    return streamBridge.send(
            producerBinding,
            MessageBuilder.withPayload(enroll).build()
    );
  }

  @Override
  public boolean unEnrollToken(HashPan hashPan, HashPan token, String par, Set<SourceApp> applications) {
    final var virtualRevokeEvent = new VirtualRevoke(hashPan.getValue(), token.getValue(), par, new Date(), applications).asCloudEvent();
    return streamBridge.send(
            producerBinding,
            MessageBuilder.withPayload(virtualRevokeEvent).build()
    );
  }
}
