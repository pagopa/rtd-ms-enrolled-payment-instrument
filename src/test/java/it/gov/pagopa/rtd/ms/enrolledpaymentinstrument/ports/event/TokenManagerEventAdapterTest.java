package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.TkmPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.FailedToNotifyRevoke;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.KafkaConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.KafkaContainerTestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.TestUtils;
import java.time.Duration;
import java.util.List;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("kafka-test")
@ExtendWith(SpringExtension.class)
@Import({
    TestChannelBinderConfiguration.class,
    TokenManagerEventAdapter.class, KafkaTestConfiguration.class, KafkaConfiguration.class
})
class TokenManagerEventAdapterTest {

  private static final String INPUT_TOPIC = "rtd-split-by-pi";

  @Autowired
  private InputDestination inputDestination;

  @Autowired
  private TkmPaymentInstrumentService tkmPaymentInstrumentService;

  @AfterEach
  void teardown() {
    Mockito.reset(tkmPaymentInstrumentService);
  }

  @Test
  void whenTkmUpdateACardThenExecuteValidUpdateCommand() throws JsonProcessingException {
    final var captor = ArgumentCaptor.forClass(TkmUpdateCommand.class);
    final var event = CloudEvent.<TokenManagerCardChanged>builder()
        .withType(TokenManagerCardChanged.TYPE)
        .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.INSERT_UPDATE).build())
        .build();
    sendMessageAsJson(event, INPUT_TOPIC);

    Mockito.verify(tkmPaymentInstrumentService).handle(captor.capture());
    assertEquals(captor.getValue().getPar(), event.getData().getPar());
    assertEquals(captor.getValue().getHashPan(), event.getData().getHashPan());
    assertThat(captor.getValue().getTokens()).hasSameElementsAs(
        event.getData().toTkmTokenCommand());
  }

  @Test
  void whenTkmUpdateACardWithNullTokensThenExecuteValidUpdateCommand()
      throws JsonProcessingException {
    final var captor = ArgumentCaptor.forClass(TkmUpdateCommand.class);
    final var event = CloudEvent.<TokenManagerCardChanged>builder()
        .withType(TokenManagerCardChanged.TYPE)
        .withData(
            TestUtils.prepareRandomTokenManagerEvent(CardChangeType.INSERT_UPDATE).hashTokens(null)
                .build())
        .build();
    sendMessageAsJson(event, INPUT_TOPIC);

    Mockito.verify(tkmPaymentInstrumentService).handle(captor.capture());
    assertEquals(captor.getValue().getPar(), event.getData().getPar());
    assertEquals(captor.getValue().getHashPan(), event.getData().getHashPan());
    assertThat(captor.getValue().getTokens()).hasSameElementsAs(
        event.getData().toTkmTokenCommand());
  }

  @Test
  void whenTkmUpdateACardWithMissingMandatoryFieldsThenAdapterShouldNotCallService()
      throws JsonProcessingException {
    final var event = CloudEvent.<TokenManagerCardChanged>builder()
        .withType(TokenManagerCardChanged.TYPE)
        .withData(
            TestUtils.prepareRandomTokenManagerEvent(CardChangeType.INSERT_UPDATE).hashPan(null)
                .changeType(null).build())
        .build();
    sendMessageAsJson(event, INPUT_TOPIC);
    Mockito.verify(tkmPaymentInstrumentService, Mockito.times(0))
        .handle(Mockito.any(TkmUpdateCommand.class));
  }


  @Test
  void whenTkmRevokeACardThenAdapterCreateRevokeCommand() throws JsonProcessingException {
    final var captor = ArgumentCaptor.forClass(TkmRevokeCommand.class);
    final var event = CloudEvent.<TokenManagerCardChanged>builder()
        .withType(TokenManagerCardChanged.TYPE)
        .withData(
            TestUtils.prepareRandomTokenManagerEvent(CardChangeType.REVOKE).hashTokens(List.of())
                .build())
        .build();
    sendMessageAsJson(event, INPUT_TOPIC);

    Mockito.verify(tkmPaymentInstrumentService).handle(captor.capture());
    assertEquals(captor.getValue().getPar(), event.getData().getPar());
    assertEquals(captor.getValue().getHashPan(), event.getData().getHashPan());
    assertEquals(captor.getValue().getTaxCode(), event.getData().getTaxCode());
  }

  @Nested
  @SpringBootTest
  @DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
  @ActiveProfiles("kafka-test")
  @Testcontainers
  @Import({TokenManagerEventAdapter.class, KafkaTestConfiguration.class, KafkaConfiguration.class})
  class KafkaIntegrationConsumerTests {

    @Autowired
    TkmPaymentInstrumentService tkmPaymentInstrumentService;

    @Container
    public static final KafkaContainer kafkaContainer = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))
        .withEmbeddedZookeeper();

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
      registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
      registry.add("test.broker", kafkaContainer::getBootstrapServers);
    }

    @Value("${test.kafka.topic}")
    private String topic;

    private KafkaTemplate<String, CloudEvent<?>> kafkaTemplate;

    @BeforeEach
    void setup() {
      Mockito.reset(tkmPaymentInstrumentService);
      kafkaTemplate = new KafkaTemplate<>(
          new DefaultKafkaProducerFactory<>(KafkaContainerTestUtils.producerProps(kafkaContainer),
              new StringSerializer(), new JsonSerializer<>())
      );
      KafkaContainerTestUtils.createTopic(kafkaContainer, topic, 1);
    }

    @AfterEach
    void teardown() {
      kafkaTemplate.destroy();
      KafkaContainerTestUtils.createAdminClient(kafkaContainer).deleteTopics(List.of(topic));
    }


    @ParameterizedTest
    @ValueSource(classes = {DuplicateKeyException.class, OptimisticLockingFailureException.class})
    void whenUpdateCommandFailWithWriteConflictsThenRetryUntilMaxAttempts(Class<? extends Exception> exception) {
      Mockito.doThrow(exception)
          .when(tkmPaymentInstrumentService)
          .handle(Mockito.any(TkmUpdateCommand.class));

      kafkaTemplate.send(topic,
          CloudEvent.<TokenManagerCardChanged>builder()
              .withType(TokenManagerCardChanged.TYPE)
              .withData(
                  TestUtils.prepareRandomTokenManagerEvent(CardChangeType.INSERT_UPDATE).build())
              .build()
      );

      await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
        Mockito.verify(tkmPaymentInstrumentService, Mockito.atLeast(3))
            .handle(Mockito.any(TkmUpdateCommand.class));
      });
    }

    @ParameterizedTest
    @ValueSource(classes = {OptimisticLockingFailureException.class, DuplicateKeyException.class})
    void whenRevokeCommandFailWithWriteConflictsThenRetryUntilMaxAttempts(
        Class<? extends Exception> exception) {
      Mockito.doThrow(exception)
          .when(tkmPaymentInstrumentService)
          .handle(Mockito.any(TkmRevokeCommand.class));

      kafkaTemplate.send(
          topic,
          CloudEvent.<TokenManagerCardChanged>builder()
              .withType(TokenManagerCardChanged.TYPE)
              .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.REVOKE).build())
              .build()
      );

      await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
        Mockito.verify(tkmPaymentInstrumentService, Mockito.atLeast(3))
            .handle(Mockito.any(TkmRevokeCommand.class));
      });
    }

    @Test
    void whenFailToNotifyRevokeThenRetryUntilMaxAttempts() {
      Mockito.doThrow(FailedToNotifyRevoke.class)
          .when(tkmPaymentInstrumentService)
          .handle(Mockito.any(TkmRevokeCommand.class));

      kafkaTemplate.send(
          topic,
          CloudEvent.<TokenManagerCardChanged>builder()
              .withType(TokenManagerCardChanged.TYPE)
              .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.REVOKE).build())
              .build()
      );

      await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
        Mockito.verify(tkmPaymentInstrumentService, Mockito.atLeast(3))
            .handle(Mockito.any(TkmRevokeCommand.class));
      });
    }

  }

  private <T> void sendMessageAsJson(T payload, String inputName) throws JsonProcessingException {
    inputDestination.send(
        MessageBuilder.withPayload(new ObjectMapper().writeValueAsString(payload)).build(),
        inputName
    );
  }

}