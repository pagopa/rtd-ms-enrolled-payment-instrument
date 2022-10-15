package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.messaging.Message;

import java.util.*;
import java.util.function.Predicate;

/**
 * Cause rtd-enrolled-pi kafka queue is used to publish event from specific application
 * and for tkm updates, this router allow to re-route the message to proper consumer by keeping SRP
 * in every consumer.
 */
@Slf4j
public class PaymentInstrumentEventRouter implements MessageRoutingCallback {

  private final String tkmUpdateConsumerName;
  private final String enrolledInstrumentConsumerName;
  private final ObjectMapper objectMapper;

  private static final Predicate<Map<String, Object>> TKM_FILTER =
          map -> map.containsKey("par") || map.containsKey("htokens");

  public PaymentInstrumentEventRouter(
          String tkmUpdateConsumerName,
          String enrolledInstrumentConsumerName,
          ObjectMapper objectMapper
  ) {
    Objects.requireNonNull(tkmUpdateConsumerName);
    Objects.requireNonNull(enrolledInstrumentConsumerName);
    this.tkmUpdateConsumerName = tkmUpdateConsumerName;
    this.enrolledInstrumentConsumerName = enrolledInstrumentConsumerName;
    this.objectMapper = objectMapper;
  }

  @Override
  public FunctionRoutingResult routingResult(Message<?> message) {
    try {
      final var rawPayload = message.getPayload();
      final var typeRef = new TypeReference<HashMap<String, Object>>() {};
      Optional<Map<String, Object>> payload;

      if (rawPayload instanceof String) {
        payload = Optional.ofNullable(objectMapper.readValue(rawPayload.toString(), typeRef));
      } else if (rawPayload instanceof byte[]) {
        payload = Optional.ofNullable(objectMapper.readValue(new String((byte[]) rawPayload), typeRef));
      } else {
        payload = Optional.empty();
      }

      return payload
              .map(it -> TKM_FILTER.test(it) ? tkmUpdateConsumerName : enrolledInstrumentConsumerName)
              .map(FunctionRoutingResult::new)
              .orElseThrow(() -> new UnknownFormatConversionException("Unknown format " + rawPayload.getClass()));

    } catch (JsonProcessingException | UnknownFormatConversionException exception) {
      log.warn("Unknown event or fail during parse as json", exception);
      throw new UnknownFormatConversionException(exception.getMessage());
    }
  }
}
