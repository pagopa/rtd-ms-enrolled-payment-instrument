package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.routes;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationEnrollEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest()
@EnableAutoConfiguration(exclude = EmbeddedMongoAutoConfiguration.class)
@TestPropertySource(properties = {"spring.config.location=classpath:application-test.yml"}, inheritProperties = false)
public class PaymentInstrumentEventRouterTest {

  private static final String TKM_CONSUMER_DESTINATION = "tkmConsumer";
  private static final String APPLICATION_CONSUMER_DESTINATION = "appConsumer";

  private PaymentInstrumentEventRouter eventRouter;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    this.eventRouter = new PaymentInstrumentEventRouter(TKM_CONSUMER_DESTINATION, APPLICATION_CONSUMER_DESTINATION, objectMapper);
  }

  @Test
  void whenReceiveParOrHashTokenPayloadThenRedirectToTkmConsumer() throws JsonProcessingException {
    final var tkmEvent = TokenManagerCardChanged.builder()
            .par("123")
            .hashTokens(List.of())
            .changeType(CardChangeType.REVOKE)
            .build();
    final var destination = eventRouter.routingResult(
            MessageBuilder.withPayload(objectMapper.writeValueAsString(tkmEvent)).build()
    );
    assertEquals(TKM_CONSUMER_DESTINATION, destination.getFunctionDefinition());
  }

  @Test
  void whenReceiveApplicationEventPayloadThenRedirectToApplicationConsumer() throws JsonProcessingException {
    final var applicationEvent = ApplicationEnrollEvent.builder()
            .operation("CREATE")
            .hashPan(TestUtils.generateRandomHashPan().getValue())
            .app("FA")
            .build();
    final var destination = eventRouter.routingResult(
            MessageBuilder.withPayload(objectMapper.writeValueAsString(applicationEvent)).build()
    );
    assertEquals(APPLICATION_CONSUMER_DESTINATION, destination.getFunctionDefinition());
  }

  @Test
  void whenReceiveBytePayloadThenSuccessfullyParseAndRedirect() throws JsonProcessingException {
    final var event = TokenManagerCardChanged.builder().par("123").build();
    final var destination = eventRouter.routingResult(
            MessageBuilder.withPayload(objectMapper.writeValueAsBytes(event)).build()
    );
    assertEquals(TKM_CONSUMER_DESTINATION, destination.getFunctionDefinition());
  }
}
