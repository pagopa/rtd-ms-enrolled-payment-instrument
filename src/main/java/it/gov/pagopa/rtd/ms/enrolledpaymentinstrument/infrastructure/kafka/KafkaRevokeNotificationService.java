package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Date;

public class KafkaRevokeNotificationService implements InstrumentRevokeNotificationService {

  private static final String PRODUCER_BINDING = "rtdRevokedPi-out-0";

  private final StreamBridge streamBridge;

  public KafkaRevokeNotificationService(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  @Override
  public boolean notifyRevoke(String taxCode, HashPan hashPan) {
    return streamBridge.send(PRODUCER_BINDING,
            MessageBuilder
                    .withPayload(new RevokeNotification(taxCode, hashPan.getValue(), new Date()))
                    .build()
    );
  }
}
