package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.EnrollAckError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.KafkaConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationInstrumentAdded;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationInstrumentDeleted;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.validation.ConstraintViolationException;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@EmbeddedKafka(topics = {"${test.kafka.topic}"}, partitions = 1, bootstrapServersProperty = "spring.embedded.kafka.brokers")
@Import({KafkaTestConfiguration.class, KafkaConfiguration.class})
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ExtendWith(MockitoExtension.class)
class ApplicationInstrumentEventAdapterTest {

  private static final String DEFAULT_APPLICATION = "ID_PAY";
  private static final String BINDING_NAME = "functionRouter-in-0";

  @Value("${test.kafka.topic}")
  private String topic;

  @Autowired
  private StreamBridge stream;

  @Autowired
  private EnrolledPaymentInstrumentService paymentInstrumentService;

  @Autowired
  private EnrolledPaymentInstrumentRepository repository;

  private KafkaTemplate<String, CloudEvent<?>> kafkaTemplate;

  @BeforeEach
  void setup(@Autowired EmbeddedKafkaBroker broker) {
    Mockito.reset(paymentInstrumentService);
    kafkaTemplate = new KafkaTemplate<>(
            new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(broker), new StringSerializer(), new JsonSerializer<>())
    );
  }

  @AfterEach
  void teardown() {
    kafkaTemplate.destroy();
  }

  @Test
  void whenReceiveInstrumentAddedThenCreateApplicationCommandBasedOnEvent() {
    final var captor = ArgumentCaptor.forClass(EnrollPaymentInstrumentCommand.class);
    final var hashPan = TestUtils.generateRandomHashPanAsString();
    final var event = CloudEvent.builder()
            .withType(ApplicationInstrumentAdded.TYPE)
            .withData(new ApplicationInstrumentAdded(hashPan, true, DEFAULT_APPLICATION))
            .build();

    final var isSent = stream.send(BINDING_NAME, event);

    assertTrue(isSent);
    Mockito.verify(paymentInstrumentService).handle(captor.capture());

    assertEquals(hashPan, captor.getValue().getHashPan());
    assertEquals(DEFAULT_APPLICATION, captor.getValue().getSourceApp());
    assertEquals(Operation.CREATE, captor.getValue().getOperation());
  }

  @Test
  void whenReceiveInstrumentDeletedThenCreateApplicationCommandBasedOnEvent() {
    final var captor = ArgumentCaptor.forClass(EnrollPaymentInstrumentCommand.class);
    final var hashPan = TestUtils.generateRandomHashPanAsString();
    final var event = CloudEvent.builder()
            .withType(ApplicationInstrumentDeleted.TYPE)
            .withData(new ApplicationInstrumentDeleted(hashPan, true, DEFAULT_APPLICATION))
            .build();

    final var isSent = stream.send(BINDING_NAME, event);

    assertTrue(isSent);
    Mockito.verify(paymentInstrumentService).handle(captor.capture());

    assertEquals(hashPan, captor.getValue().getHashPan());
    assertEquals(DEFAULT_APPLICATION, captor.getValue().getSourceApp());
    assertEquals(Operation.DELETE, captor.getValue().getOperation());
  }

  @Test
  void whenEventMissMandatoryFieldThenThrowsException() {
    final var message = CloudEvent.builder()
            .withType(ApplicationInstrumentAdded.TYPE)
            .withData(new ApplicationInstrumentAdded(null, true, DEFAULT_APPLICATION))
            .build();

    final var exception = assertThrows(MessageHandlingException.class, () -> stream.send(BINDING_NAME, message));

    assertTrue(exception.getCause() instanceof ConstraintViolationException);
  }

  @Test
  void whenReceivedMalformedEventThenRejectIt() {
    Mockito.doNothing().when(paymentInstrumentService).handle(Mockito.any());
    kafkaTemplate.send(topic, CloudEvent.builder().withType("").withData("").build());

    await().during(Duration.ofSeconds(3)).untilAsserted(() -> {
      Mockito.verify(paymentInstrumentService, Mockito.times(0)).handle(Mockito.any());
    });
  }

  @ParameterizedTest
  @ValueSource(classes = {OptimisticLockingFailureException.class, DuplicateKeyException.class})
  void whenServiceFailWithWriteConflictsThenRetryContinuously(Class<? extends Exception> exception) {
    final var event = CloudEvent.builder().withType(ApplicationInstrumentAdded.TYPE)
            .withData(new ApplicationInstrumentAdded(TestUtils.generateRandomHashPanAsString(), false, DEFAULT_APPLICATION))
            .build();
    Mockito.doThrow(exception)
            .when(paymentInstrumentService)
            .handle(Mockito.any());

    kafkaTemplate.send(topic, event);

    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(paymentInstrumentService, Mockito.atLeast(3)).handle(Mockito.any());
    });
  }

  @Test
  void whenServiceFailsWithTransientErrorThenRetryUntilSucceed() {
    final var event = CloudEvent.builder().withType(ApplicationInstrumentAdded.TYPE)
            .withData(new ApplicationInstrumentAdded(TestUtils.generateRandomHashPanAsString(), false, DEFAULT_APPLICATION))
            .build();

    Mockito.doThrow(OptimisticLockingFailureException.class)
            .doThrow(DuplicateKeyException.class)
            .doNothing()
            .when(paymentInstrumentService)
            .handle(Mockito.any());

    kafkaTemplate.send(topic, event);

    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(paymentInstrumentService, Mockito.timeout(15000).times(3)).handle(Mockito.any());
    });
  }

  @Test
  void whenServiceFailToAckThenRetryUntilSucceed() {
    final var event = CloudEvent.builder().withType(ApplicationInstrumentAdded.TYPE)
            .withData(new ApplicationInstrumentAdded(TestUtils.generateRandomHashPanAsString(), false, DEFAULT_APPLICATION))
            .build();

    Mockito.doThrow(EnrollAckError.class)
            .doThrow(EnrollAckError.class)
            .doNothing()
            .when(paymentInstrumentService)
            .handle(Mockito.any());

    kafkaTemplate.send(topic, event);

    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(paymentInstrumentService, Mockito.timeout(15000).times(3)).handle(Mockito.any());
    });
  }
}