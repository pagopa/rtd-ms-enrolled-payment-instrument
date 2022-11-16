package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.FailedToNotifyRevoke;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.VirtualEnrollError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.KafkaConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.TkmPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.ApplicationInstrumentAdded;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@EmbeddedKafka(topics = {"${test.kafka.topic}"}, partitions = 1, bootstrapServersProperty = "spring.embedded.kafka.brokers")
@TestPropertySource(properties = {"spring.config.location=classpath:application-test.yml"}, inheritProperties = false)
@Import(value = {KafkaTestConfiguration.class, KafkaConfiguration.class})
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class})
class TokenManagerEventAdapterTest {

  private static final int DEFAULT_AT_MOST_TIMEOUT = 10; // seconds

  @Value("${test.kafka.topic}")
  private String topic;

  private KafkaTemplate<String, CloudEvent<?>> kafkaTemplate;

  @Autowired
  private TkmPaymentInstrumentService service;

  @BeforeEach
  void setup(@Autowired EmbeddedKafkaBroker broker) {
    Mockito.reset(service);
    kafkaTemplate = new KafkaTemplate<>(
            new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(broker), new StringSerializer(), new JsonSerializer<>())
    );
  }

  @AfterEach
  void teardown() {
    kafkaTemplate.destroy();
  }

  @Test
  void whenTkmUpdateACardThenExecuteValidUpdateCommand() {
    final var captor = ArgumentCaptor.forClass(TkmUpdateCommand.class);
    final var event = CloudEvent.<TokenManagerCardChanged>builder()
            .withType(TokenManagerCardChanged.TYPE)
            .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.INSERT_UPDATE).build())
            .build();
    kafkaTemplate.send(topic, event);

    await().atMost(Duration.ofSeconds(DEFAULT_AT_MOST_TIMEOUT)).untilAsserted(() -> {
      Mockito.verify(service).handle(captor.capture());

      assertEquals(captor.getValue().getPar(), event.getData().getPar());
      assertEquals(captor.getValue().getHashPan(), event.getData().getHashPan());
      assertThat(captor.getValue().getTokens()).hasSameElementsAs(event.getData().toTkmTokenCommand());
    });
  }

  @Test
  void whenTkmUpdateACardWithNullTokensThenExecuteValidUpdateCommand() {
    final var captor = ArgumentCaptor.forClass(TkmUpdateCommand.class);
    final var event = CloudEvent.<TokenManagerCardChanged>builder()
            .withType(TokenManagerCardChanged.TYPE)
            .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.INSERT_UPDATE).hashTokens(null).build())
            .build();
    kafkaTemplate.send(topic, event);

    await().atMost(Duration.ofSeconds(DEFAULT_AT_MOST_TIMEOUT)).untilAsserted(() -> {
      Mockito.verify(service).handle(captor.capture());

      assertEquals(captor.getValue().getPar(), event.getData().getPar());
      assertEquals(captor.getValue().getHashPan(), event.getData().getHashPan());
      assertThat(captor.getValue().getTokens()).hasSameElementsAs(event.getData().toTkmTokenCommand());
    });
  }

  @Test
  void whenTkmUpdateACardWithMissingMandatoryFieldsThenAdapterShouldNotCallService() {
    final var event = CloudEvent.<TokenManagerCardChanged>builder()
            .withType(TokenManagerCardChanged.TYPE)
            .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.INSERT_UPDATE).hashPan(null).changeType(null).build())
            .build();
    kafkaTemplate.send(topic, event);

    await().during(Duration.ofSeconds(3)).untilAsserted(() -> {
      Mockito.verify(service, Mockito.times(0)).handle(Mockito.any(TkmUpdateCommand.class));
    });
  }

  @ParameterizedTest
  @ValueSource(classes = {OptimisticLockingFailureException.class, DuplicateKeyException.class})
  void whenUpdateCommandFailWithWriteConflictsThenRetryContinuously(Class<? extends Exception> exception) {
    Mockito.doThrow(exception)
            .when(service)
            .handle(Mockito.any(TkmUpdateCommand.class));

    kafkaTemplate.send(
            topic,
            CloudEvent.<TokenManagerCardChanged>builder()
                    .withType(TokenManagerCardChanged.TYPE)
                    .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.INSERT_UPDATE).build())
                    .build()
    );

    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(service, Mockito.atLeast(3)).handle(Mockito.any(TkmUpdateCommand.class));
    });
  }

  @Test
  void whenTkmRevokeACardThenAdapterCreateRevokeCommand() {
    final var captor = ArgumentCaptor.forClass(TkmRevokeCommand.class);
    final var event = CloudEvent.<TokenManagerCardChanged>builder()
            .withType(TokenManagerCardChanged.TYPE)
            .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.REVOKE).hashTokens(List.of()).build())
            .build();
    kafkaTemplate.send(topic, event);

    await().atMost(Duration.ofSeconds(DEFAULT_AT_MOST_TIMEOUT)).untilAsserted(() -> {
      Mockito.verify(service).handle(captor.capture());

      assertEquals(captor.getValue().getPar(), event.getData().getPar());
      assertEquals(captor.getValue().getHashPan(), event.getData().getHashPan());
      assertEquals(captor.getValue().getTaxCode(), event.getData().getTaxCode());
    });
  }

  @ParameterizedTest
  @ValueSource(classes = {OptimisticLockingFailureException.class, DuplicateKeyException.class})
  void whenRevokeCommandFailWithWriteConflictsThenRetryContinuously(Class<? extends Exception> exception) {
    Mockito.doThrow(exception)
            .when(service)
            .handle(Mockito.any(TkmRevokeCommand.class));

    kafkaTemplate.send(
            topic,
            CloudEvent.<TokenManagerCardChanged>builder()
                    .withType(TokenManagerCardChanged.TYPE)
                    .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.REVOKE).build())
                    .build()
    );

    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(service, Mockito.atLeast(3)).handle(Mockito.any(TkmRevokeCommand.class));
    });
  }

  @Test
  void whenFailToNotifyRevokeThenRetryContinuously() {
    Mockito.doThrow(FailedToNotifyRevoke.class)
            .when(service)
            .handle(Mockito.any(TkmRevokeCommand.class));

    kafkaTemplate.send(
            topic,
            CloudEvent.<TokenManagerCardChanged>builder()
                    .withType(TokenManagerCardChanged.TYPE)
                    .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.REVOKE).build())
                    .build()
    );

    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(service, Mockito.atLeast(3)).handle(Mockito.any(TkmRevokeCommand.class));
    });
  }

  @Test
  void whenServiceFailToVirtualEnrollThenRetryUntilSucceed() {
    final var hashPan = TestUtils.generateRandomHashPanAsString();
    final var tokenUpdate = CloudEvent.<TokenManagerCardChanged>builder()
            .withType(TokenManagerCardChanged.TYPE)
            .withData(new TokenManagerCardChanged(hashPan, "taxCode", "par", List.of(), LocalDateTime.now(), CardChangeType.INSERT_UPDATE))
            .build();

    Mockito.doThrow(VirtualEnrollError.class)
            .doThrow(VirtualEnrollError.class)
            .doNothing()
            .when(service)
            .handle(Mockito.any(TkmUpdateCommand.class));

    kafkaTemplate.send(topic, tokenUpdate);

    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(service, Mockito.timeout(15000).times(3)).handle(Mockito.any(TkmUpdateCommand.class));
    });
  }

}