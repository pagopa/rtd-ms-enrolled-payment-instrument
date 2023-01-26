package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.revoke;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Date;
import java.util.Set;

public class KafkaRevokeNotificationService implements InstrumentRevokeNotificationService {

  private final String producerBinding;
  private final StreamBridge streamBridge;

  public KafkaRevokeNotificationService(
          String producerBinding,
          StreamBridge streamBridge
  ) {
    this.streamBridge = streamBridge;
    this.producerBinding = producerBinding;
  }

  @Override
  public boolean notifyRevoke(Set<SourceApp> apps, String taxCode, HashPan hashPan) {
    final var cloudEvent = CloudEvent.builder()
            .withType(RevokeNotification.TYPE)
            .withData(new RevokeNotification(taxCode, hashPan.getValue(), new Date(), apps))
            .build();
    return streamBridge.send(
            producerBinding,
            MessageBuilder.withPayload(cloudEvent)
                    .setHeader("partitionKey", hashPan.getValue())
                    .build()
    );
  }
}
