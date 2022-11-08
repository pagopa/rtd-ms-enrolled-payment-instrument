package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.routes;

import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class StringMessageUtils {

  public static Optional<String> convertPayloadToString(Message<?> message) {
    final var rawPayload = message.getPayload();
    if (rawPayload instanceof String) {
      return Optional.of(rawPayload.toString());
    } else if (rawPayload instanceof byte[]) {
      return Optional.of(new String((byte[]) rawPayload, StandardCharsets.UTF_8));
    }
    return Optional.empty();
  }
}
