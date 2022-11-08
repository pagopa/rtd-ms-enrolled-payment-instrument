package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.messaging.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UnknownFormatConversionException;

/**
 * Cause rtd-enrolled-pi kafka queue is used to publish event from specific application
 * and for tkm updates, this router allow to re-route the message to proper consumer by keeping SRP
 * in every consumer.
 */
@Slf4j
public class PaymentInstrumentEventRouter implements MessageRoutingCallback {

  private final Map<String, String> routingMap;
  private final TypeReference<HashMap<String, Object>> rawTypeReference;
  private final ObjectMapper objectMapper;

  public PaymentInstrumentEventRouter(
          Map<String, String> routingMap,
          ObjectMapper objectMapper
  ) {
    this.routingMap = routingMap;
    this.objectMapper = objectMapper;
    this.rawTypeReference = new TypeReference<>() {
    };
  }

  @Override
  public FunctionRoutingResult routingResult(Message<?> message) {
    try {
      return StringMessageUtils.convertPayloadToString(message)
              .flatMap(this::parseToJson)
              .flatMap(it -> Optional.ofNullable(it.get("type").toString()))
              .flatMap(it -> Optional.ofNullable(routingMap.get(it)))
              .map(FunctionRoutingResult::new)
              .orElseThrow(() -> new UnknownFormatConversionException("Unknown format " + message.getPayload().getClass()));

    } catch (UnknownFormatConversionException exception) {
      log.warn("Unknown event or fail during parse as json", exception);
      throw new UnknownFormatConversionException(exception.getMessage());
    }
  }

  private Optional<Map<String, Object>> parseToJson(String payload) {
    try {
      return Optional.ofNullable(objectMapper.readValue(payload, rawTypeReference));
    } catch (JsonProcessingException e) {
      log.warn("Fail during parse as json", e);
      return Optional.empty();
    }
  }
}
