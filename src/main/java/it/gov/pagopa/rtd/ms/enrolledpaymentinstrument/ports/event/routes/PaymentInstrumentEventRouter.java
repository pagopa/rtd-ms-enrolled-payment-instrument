package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.messaging.Message;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Predicate;

/**
 * Cause rtd-enrolled-pi kafka queue is used to publish event from specific application
 * and for tkm updates, this router allow to re-route the message to proper consumer by keeping SRP
 * in every consumer.
 */
@Slf4j
public class PaymentInstrumentEventRouter implements MessageRoutingCallback {

  private final TypeReference<CloudEvent<?>> cloudEventTypeReference;
  private final Map<String, String> routingMap;
  private final TypeReference<HashMap<String, Object>> rawTypeReference;
  private final ObjectMapper objectMapper;

  public PaymentInstrumentEventRouter(
          Map<String, String> routingMap,
          ObjectMapper objectMapper
  ) {
    this.routingMap = routingMap;
    this.objectMapper = objectMapper;
    this.cloudEventTypeReference = new TypeReference<>() {
    };
    this.rawTypeReference = new TypeReference<>() {
    };
  }

  @Override
  public FunctionRoutingResult routingResult(Message<?> message) {
    try {
      final var rawPayload = message.getPayload();
      Optional<Map<String, Object>> payload;

      if (rawPayload instanceof String) {
        payload = Optional.ofNullable(objectMapper.readValue(rawPayload.toString(), rawTypeReference));
      } else if (rawPayload instanceof byte[]) {
        payload = Optional.ofNullable(objectMapper.readValue(new String((byte[]) rawPayload), rawTypeReference));
      } else {
        payload = Optional.empty();
      }

      return payload
              .flatMap(it -> Optional.ofNullable(it.get("type").toString()))
              .flatMap(it -> Optional.ofNullable(routingMap.get(it)))
              .map(FunctionRoutingResult::new)
              .orElseThrow(() -> new UnknownFormatConversionException("Unknown format " + rawPayload.getClass()));

    } catch (JsonProcessingException | UnknownFormatConversionException exception) {
      log.warn("Unknown event or fail during parse as json", exception);
      throw new UnknownFormatConversionException(exception.getMessage());
    }
  }
}
