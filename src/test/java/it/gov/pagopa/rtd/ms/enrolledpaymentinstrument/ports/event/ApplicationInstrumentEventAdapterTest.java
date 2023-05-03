package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.EnrollAckError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.KafkaConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationInstrumentAdded;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationInstrumentDeleted;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.KafkaContainerTestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.TestUtils;
import jakarta.validation.ConstraintViolationException;
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
import org.springframework.cloud.stream.binder.test.TestChannelBinder;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.support.ErrorMessage;
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
    TokenManagerEventAdapter.class,
    KafkaTestConfiguration.class,
    KafkaConfiguration.class
})
class ApplicationInstrumentEventAdapterTest {

  private static final String DEFAULT_APPLICATION = "ID_PAY";
  private static final String INPUT_NAME = "rtd-split-by-pi";

  @Autowired
  private InputDestination inputDestination;

  @Autowired
  private TestChannelBinder testChannelBinder;

  @Autowired
  EnrolledPaymentInstrumentService paymentInstrumentService;

  @AfterEach
  void afterEach() {
    reset(paymentInstrumentService);
  }

  @Test
  void whenReceiveInstrumentAddedThenCreateApplicationCommandBasedOnEvent()
      throws JsonProcessingException {
    final var captor = ArgumentCaptor.forClass(EnrollPaymentInstrumentCommand.class);
    final var hashPan = TestUtils.generateRandomHashPanAsString();
    final var event = CloudEvent.builder()
        .withType(ApplicationInstrumentAdded.TYPE)
        .withData(new ApplicationInstrumentAdded(hashPan, true, DEFAULT_APPLICATION))
        .build();

    sendMessageAsJson(event, INPUT_NAME);

    Mockito.verify(paymentInstrumentService).handle(captor.capture());

    assertEquals(hashPan, captor.getValue().getHashPan());
    assertEquals(DEFAULT_APPLICATION, captor.getValue().getSourceApp());
    assertEquals(Operation.CREATE, captor.getValue().getOperation());
  }

  @Test
  void whenReceiveInstrumentDeletedThenCreateApplicationCommandBasedOnEvent()
      throws JsonProcessingException {
    final var captor = ArgumentCaptor.forClass(EnrollPaymentInstrumentCommand.class);
    final var hashPan = TestUtils.generateRandomHashPanAsString();
    final var event = CloudEvent.builder()
        .withType(ApplicationInstrumentDeleted.TYPE)
        .withData(new ApplicationInstrumentDeleted(hashPan, true, DEFAULT_APPLICATION))
        .build();

    sendMessageAsJson(event, INPUT_NAME);

    Mockito.verify(paymentInstrumentService).handle(captor.capture());

    assertEquals(hashPan, captor.getValue().getHashPan());
    assertEquals(DEFAULT_APPLICATION, captor.getValue().getSourceApp());
    assertEquals(Operation.DELETE, captor.getValue().getOperation());
  }

  @Test
  void whenEventMissMandatoryFieldThenThrowsException() throws JsonProcessingException {
    final var message = CloudEvent.builder()
        .withType(ApplicationInstrumentAdded.TYPE)
        .withData(new ApplicationInstrumentAdded(null, true, DEFAULT_APPLICATION))
        .build();

    sendMessageAsJson(message, INPUT_NAME);

    final var lastError = (ErrorMessage) testChannelBinder.getLastError();
    assertTrue(lastError.getPayload().getCause() instanceof ConstraintViolationException);
  }

  @Test
  void whenReceivedMalformedEventThenRejectIt() throws JsonProcessingException {
    Mockito.doNothing().when(paymentInstrumentService)
        .handle(any(EnrollPaymentInstrumentCommand.class));
    sendMessageAsJson(CloudEvent.builder().withType("").withData("").build(), INPUT_NAME);

    await().during(Duration.ofSeconds(3)).untilAsserted(() -> {
      Mockito.verify(paymentInstrumentService, times(0))
          .handle(any(EnrollPaymentInstrumentCommand.class));
    });
  }

  @Nested
  @SpringBootTest
  @DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
  @ActiveProfiles("kafka-test")
  @Testcontainers
  @Import({TokenManagerEventAdapter.class, KafkaTestConfiguration.class, KafkaConfiguration.class})
  class KafkaIntegrationConsumerTests {

    @Autowired
    EnrolledPaymentInstrumentService paymentInstrumentService;

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
      Mockito.reset(paymentInstrumentService);
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
    @ValueSource(classes = {OptimisticLockingFailureException.class, DuplicateKeyException.class})
    void whenServiceFailWithWriteConflictsThenRetryUntilMaxRetry(
        Class<? extends Exception> exception) {
      final var event = CloudEvent.builder().withType(ApplicationInstrumentAdded.TYPE)
          .withData(new ApplicationInstrumentAdded(TestUtils.generateRandomHashPanAsString(), false,
              DEFAULT_APPLICATION))
          .build();
      Mockito.doThrow(exception)
          .when(paymentInstrumentService)
          .handle(any(EnrollPaymentInstrumentCommand.class));

      kafkaTemplate.send(topic, event);

      await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
        Mockito.verify(paymentInstrumentService, Mockito.atLeast(3))
            .handle(any(EnrollPaymentInstrumentCommand.class));
      });
    }

    @Test
    void whenServiceFailsWithTransientErrorThenRetryUntilSucceedOrMaxRetry() {
      final var event = CloudEvent.builder().withType(ApplicationInstrumentAdded.TYPE)
          .withData(new ApplicationInstrumentAdded(TestUtils.generateRandomHashPanAsString(), false,
              DEFAULT_APPLICATION))
          .build();

      Mockito.doThrow(OptimisticLockingFailureException.class)
          .doThrow(DuplicateKeyException.class)
          .doNothing()
          .when(paymentInstrumentService)
          .handle(any(EnrollPaymentInstrumentCommand.class));

      kafkaTemplate.send(topic, event);

      await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
        Mockito.verify(paymentInstrumentService, times(3))
            .handle(any(EnrollPaymentInstrumentCommand.class));
      });
    }

    @Test
    void whenServiceFailToAckThenRetryUntilSucceedOrMaxAttempts() {
      final var event = CloudEvent.builder().withType(ApplicationInstrumentAdded.TYPE)
          .withData(new ApplicationInstrumentAdded(TestUtils.generateRandomHashPanAsString(), false,
              DEFAULT_APPLICATION))
          .build();

      Mockito.doThrow(EnrollAckError.class)
          .doThrow(EnrollAckError.class)
          .doNothing()
          .when(paymentInstrumentService)
          .handle(any(EnrollPaymentInstrumentCommand.class));

      kafkaTemplate.send(topic, event);

      await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
        Mockito.verify(paymentInstrumentService, times(3))
            .handle(any(EnrollPaymentInstrumentCommand.class));
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