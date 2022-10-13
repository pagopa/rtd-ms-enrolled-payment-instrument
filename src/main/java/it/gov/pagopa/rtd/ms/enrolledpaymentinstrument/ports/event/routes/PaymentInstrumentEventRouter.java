package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.routes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.cloud.function.context.MessageRoutingCallback;
import org.springframework.messaging.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Cause rtd-enrolled-pi kafka queue is used to publish event from specific application
 * and for tkm updates, this router allow to re-route the message to proper consumer by keeping SRP
 * in every consumer.
 */
public class PaymentInstrumentEventRouter implements MessageRoutingCallback  {

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
    this.objectMapper =  objectMapper;
  }

  @SneakyThrows
  @Override
  public FunctionRoutingResult routingResult(Message<?> message) {
    final var typeRef = new TypeReference<HashMap<String,Object>>() {};
    final var payload = objectMapper.readValue(message.getPayload().toString(), typeRef);
    if (TKM_FILTER.test(payload)) {
      return new FunctionRoutingResult(tkmUpdateConsumerName);
    } else {
      return new FunctionRoutingResult(enrolledInstrumentConsumerName);
    }
  }
}
