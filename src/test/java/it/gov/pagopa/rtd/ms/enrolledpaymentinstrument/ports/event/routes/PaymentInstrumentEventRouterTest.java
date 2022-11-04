package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.routes;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationEnrollEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Objects;
import java.util.UnknownFormatConversionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@Import({PaymentInstrumentEventRouterTest.Config.class, ValidationAutoConfiguration.class})
class PaymentInstrumentEventRouterTest {

  private static final String TKM_CONSUMER_DESTINATION = "tkmConsumer";
  private static final String APPLICATION_CONSUMER_DESTINATION = "appConsumer";

  private PaymentInstrumentEventRouter eventRouter;

  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    this.eventRouter = new PaymentInstrumentEventRouter(TKM_CONSUMER_DESTINATION, APPLICATION_CONSUMER_DESTINATION, objectMapper);
  }

  @Test
  void whenReceiveParOrHashTokenOrTaxCodePayloadThenRedirectToTkmConsumer() {
    final var tkmEvents = List.of(
            TokenManagerCardChanged.builder().par("123").hashTokens(List.of()).taxCode("123").changeType(CardChangeType.REVOKE).build(),
            TokenManagerCardChanged.builder().taxCode("123").changeType(CardChangeType.REVOKE).build(),
            TokenManagerCardChanged.builder().par("123").changeType(CardChangeType.REVOKE).build(),
            TokenManagerCardChanged.builder().hashTokens(List.of()).changeType(CardChangeType.REVOKE).build()
    );

    assertThat(tkmEvents)
            .map(it -> eventRouter.routingResult(MessageBuilder.withPayload(objectMapper.writeValueAsString(it)).build()))
            .allMatch(it -> Objects.equals(TKM_CONSUMER_DESTINATION, it.getFunctionDefinition()));
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

  @Test
  void whenReceiveNonJsonPayloadThenThrowException() {
    assertThrowsExactly(
            UnknownFormatConversionException.class,
            () -> eventRouter.routingResult(MessageBuilder.withPayload("123").build())
    );
  }

  @Test
  void whenReceiveNonStringPayloadThenThrowException() {
    assertThrowsExactly(
            UnknownFormatConversionException.class,
            () -> eventRouter.routingResult(MessageBuilder.withPayload(new Object()).build())
    );
  }

  @TestConfiguration
  public static class Config {
  }
}
