package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.routes;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.*;
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

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@Import({PaymentInstrumentEventRouterTest.Config.class, ValidationAutoConfiguration.class})
class PaymentInstrumentEventRouterTest {

  private static final Map<String, String> routingMap = new HashMap<>();
  private static final String TKM_CONSUMER_DESTINATION = "tokenManagerCardChanged";
  private static final String APPLICATION_INSTRUMENT_ADDED_DESTINATION = "applicationInstrumentAddedConsumer";
  private static final String APPLICATION_INSTRUMENT_DELETED_DESTINATION = "applicationInstrumentDeletedConsumer";
  private static final String PAYMENT_INSTRUMENT_EXPORTED_DESTINATION = "paymentInstrumentExportedConsumer";

  static {
    routingMap.put(ApplicationInstrumentAdded.TYPE, APPLICATION_INSTRUMENT_ADDED_DESTINATION);
    routingMap.put(ApplicationInstrumentDeleted.TYPE, APPLICATION_INSTRUMENT_DELETED_DESTINATION);
    routingMap.put(TokenManagerCardChanged.TYPE, TKM_CONSUMER_DESTINATION);
    routingMap.put(PaymentInstrumentExported.TYPE, PAYMENT_INSTRUMENT_EXPORTED_DESTINATION);
  }
  private final ObjectMapper objectMapper = new ObjectMapper();

  private PaymentInstrumentEventRouter eventRouter;

  @BeforeEach
  void setup() {
    this.eventRouter = new PaymentInstrumentEventRouter(routingMap, objectMapper);
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
            .map(it -> CloudEvent.builder().withType(TokenManagerCardChanged.TYPE).withData(it).build())
            .map(it -> eventRouter.routingResult(MessageBuilder.withPayload(objectMapper.writeValueAsString(it)).build()))
            .allMatch(it -> Objects.equals(TKM_CONSUMER_DESTINATION, it));
  }

  @Test
  void whenReceiveInstrumentAddedEventPayloadThenRedirectToApplicationInstrumentAddedConsumer() throws JsonProcessingException {
    final var applicationEvent = CloudEvent.builder()
            .withType(ApplicationInstrumentAdded.TYPE)
            .withData(new ApplicationInstrumentAdded(TestUtils.generateRandomHashPan().getValue(), false, "FA"))
            .build();
    final var destination = eventRouter.routingResult(
            MessageBuilder.withPayload(objectMapper.writeValueAsString(applicationEvent)).build()
    );
    assertEquals(APPLICATION_INSTRUMENT_ADDED_DESTINATION, destination);
  }

  @Test
  void whenReceiveInstrumentDeletedEventPayloadThenRedirectToApplicationInstrumentDeletedConsumer() throws JsonProcessingException {
    final var applicationEvent = CloudEvent.builder()
            .withType(ApplicationInstrumentDeleted.TYPE)
            .withData(new ApplicationInstrumentDeleted(TestUtils.generateRandomHashPan().getValue(), false, "FA"))
            .build();
    final var destination = eventRouter.routingResult(
            MessageBuilder.withPayload(objectMapper.writeValueAsString(applicationEvent)).build()
    );
    assertEquals(APPLICATION_INSTRUMENT_DELETED_DESTINATION, destination);
  }


  @Test
  void whenReceiveBytePayloadThenSuccessfullyParseAndRedirect() throws JsonProcessingException {
    final var event = CloudEvent.builder()
            .withType(TokenManagerCardChanged.TYPE)
            .withData(TokenManagerCardChanged.builder().par("123").build())
            .build();
    final var destination = eventRouter.routingResult(
            MessageBuilder.withPayload(objectMapper.writeValueAsBytes(event)).build()
    );
    assertEquals(TKM_CONSUMER_DESTINATION, destination);
  }

  @Test
  void whenReceiveExportedEventPayloadThenRedirectToExportedEventConsumer() throws JsonProcessingException {
    final var applicationEvent = CloudEvent.builder()
            .withType(PaymentInstrumentExported.TYPE)
            .withData(new PaymentInstrumentExported(TestUtils.generateRandomHashPan().getValue()))
            .build();
    final var destination = eventRouter.routingResult(
            MessageBuilder.withPayload(objectMapper.writeValueAsString(applicationEvent)).build()
    );
    assertEquals(PAYMENT_INSTRUMENT_EXPORTED_DESTINATION, destination);
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

  @Test
  void whenReceiveUnknownEventThenThrowException() {
    final var unknownEvent = CloudEvent.builder()
            .withType("unknown")
            .withData(new Object())
            .build();
    assertThrowsExactly(
            UnknownFormatConversionException.class,
            () -> eventRouter.routingResult(MessageBuilder.withPayload(unknownEvent).build())
    );
  }

  @TestConfiguration
  public static class Config {
  }
}
