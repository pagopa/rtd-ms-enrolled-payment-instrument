package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.KafkaConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.config.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.EnrolledPaymentInstrumentEvent;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.validation.ConstraintViolationException;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@EmbeddedKafka(
    topics = { "${test.kafka.topic}" },
    partitions = 1,
    bootstrapServersProperty = "spring.embedded.kafka.brokers"
)
@Import({ KafkaEnrolledInstrumentEventsAdapter.class, KafkaTestConfiguration.class, KafkaConfiguration.class })
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@Slf4j
@ExtendWith(MockitoExtension.class)
class KafkaEnrolledInstrumentEventsAdapterTest {

  private static final String BINDING_NAME = "enrolledPaymentInstrumentConsumer-in-0";

  @Value("${test.kafka.topic}")
  private String topic;

  @Autowired
  private StreamBridge stream;

  @Autowired
  EnrolledPaymentInstrumentService paymentInstrumentService;

  private KafkaTemplate<String, EnrolledPaymentInstrumentEvent> kafkaTemplate;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setup(@Autowired EmbeddedKafkaBroker broker) {
    Mockito.reset(paymentInstrumentService);
    kafkaTemplate = new KafkaTemplate<>(
            new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(broker), new StringSerializer(), new JsonSerializer<>())
    );
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  void teardown() {
    kafkaTemplate.destroy();
  }

  @Test
  void shouldCreateApplicationCommandBasedOnEvent() {
    final var captor = ArgumentCaptor.forClass(EnrollPaymentInstrumentCommand.class);
    final var message = MessageBuilder.withPayload(enabledPaymentInstrumentEvent).build();
    final var isSent = stream.send(BINDING_NAME, message);

    assertTrue(isSent);
    Mockito.verify(paymentInstrumentService).handle(captor.capture());

    assertEquals(hashPanEvent, captor.getValue().getHashPan());
    assertEquals(sourceAppEvent, captor.getValue().getSourceApp());
    assertEquals(Operation.CREATE, captor.getValue().getOperation());
  }

  @Test
  void shouldFailToCreateCommandWithMalformedEvent() {
    final var message = MessageBuilder
            .withPayload(enabledPaymentInstrumentEvent.replace("CREATE", "123"))
            .build();

    final var exception = assertThrows(MessageHandlingException.class, () -> stream.send(BINDING_NAME, message));

    assertTrue(exception.getCause() instanceof IllegalArgumentException);
  }

  @Test
  void mustNotHandleEventWhenMissingMandatoryEventField() {
    final var message = MessageBuilder
            .withPayload(enabledPaymentInstrumentEvent.replace("CREATE", ""))
            .build();

    final var exception = assertThrows(MessageHandlingException.class, () -> stream.send(BINDING_NAME, message));

    assertTrue(exception.getCause() instanceof ConstraintViolationException);
  }

  @Test
  void whenReceivedMalformedEventThenRejectIt() {
    Mockito.doNothing().when(paymentInstrumentService).handle(Mockito.any());
    kafkaTemplate.send(topic, new EnrolledPaymentInstrumentEvent());

    await().during(Duration.ofSeconds(3)).untilAsserted(() -> {
      Mockito.verify(paymentInstrumentService, Mockito.times(0)).handle(Mockito.any());
    });
  }

  @ParameterizedTest
  @ValueSource(classes = {OptimisticLockingFailureException.class, DuplicateKeyException.class})
  void whenServiceFailWithWriteConflictsThenRetryContinuously(Class<? extends Exception> exception) throws JsonProcessingException {
    Mockito.doThrow(exception)
            .when(paymentInstrumentService)
            .handle(Mockito.any());

    kafkaTemplate.send(topic, objectMapper.readValue(enabledPaymentInstrumentEvent, EnrolledPaymentInstrumentEvent.class));

    await().pollDelay(Duration.ofSeconds(10)).atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(paymentInstrumentService, Mockito.atLeast(3)).handle(Mockito.any());
    });
  }

  @Test
  void whenServiceFailsWithTransientErrorThenRetryUntilSucceed() throws JsonProcessingException, InterruptedException {
    Mockito.doThrow(OptimisticLockingFailureException.class)
            .doThrow(DuplicateKeyException.class)
            .doNothing()
            .when(paymentInstrumentService)
            .handle(Mockito.any());

    kafkaTemplate.send(topic, objectMapper.readValue(enabledPaymentInstrumentEvent, EnrolledPaymentInstrumentEvent.class));

    await().pollDelay(Duration.ofSeconds(10)).atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(paymentInstrumentService, Mockito.timeout(15000).times(3)).handle(Mockito.any());
    });

    System.out.println( Mockito.mockingDetails(paymentInstrumentService).getInvocations());
  }

  private static final String hashPanEvent = "42771c850db05733b749d7e05153d0b8c77b54949d99740343696bc483a07aba";
  private static final String sourceAppEvent = "FA";
  private static final String enabledPaymentInstrumentEvent = ""
      + "{\n"
      + "  \"hashPan\": \"" + hashPanEvent + "\",\n"
      + "  \"app\": \"" + sourceAppEvent + "\",\n"
      + "  \"operation\": \"CREATE\"\n"
      + "}";
}