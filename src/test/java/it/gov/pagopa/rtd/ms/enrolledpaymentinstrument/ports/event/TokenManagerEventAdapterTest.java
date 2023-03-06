package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.TkmPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.FailedToNotifyRevoke;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.KafkaConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("kafka-test")
@EmbeddedKafka(bootstrapServersProperty = "spring.embedded.kafka.brokers", partitions = 1)
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
@Import({TokenManagerEventAdapter.class, KafkaTestConfiguration.class, KafkaConfiguration.class})
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class})
class TokenManagerEventAdapterTest {

  private static final int DEFAULT_AT_MOST_TIMEOUT = 10; // seconds

  @Value("${test.kafka.topic}")
  private String topic;

  @Autowired
  private TkmPaymentInstrumentService tkmPaymentInstrumentService;

  private KafkaTemplate<String, CloudEvent<?>> kafkaTemplate;

  @BeforeEach
  void setup(@Autowired EmbeddedKafkaBroker broker) {
    Mockito.reset(tkmPaymentInstrumentService);
    kafkaTemplate = new KafkaTemplate<>(
            new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(broker), new StringSerializer(), new JsonSerializer<>())
    );
    broker.addTopicsWithResults(topic);
  }

  @AfterEach
  void teardown(@Autowired EmbeddedKafkaBroker broker) {
    kafkaTemplate.destroy();
    broker.doWithAdmin(admin -> admin.deleteTopics(List.of(topic)));
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
      Mockito.verify(tkmPaymentInstrumentService).handle(captor.capture());

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
      Mockito.verify(tkmPaymentInstrumentService).handle(captor.capture());

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
      Mockito.verify(tkmPaymentInstrumentService, Mockito.times(0)).handle(Mockito.any(TkmUpdateCommand.class));
    });
  }

  @ParameterizedTest
  @ValueSource(classes = {DuplicateKeyException.class, OptimisticLockingFailureException.class})
  void whenUpdateCommandFailWithWriteConflictsThenRetryUntilMaxAttempts(Class<? extends Exception> exception) {
    Mockito.doThrow(exception)
            .when(tkmPaymentInstrumentService)
            .handle(Mockito.any(TkmUpdateCommand.class));

    kafkaTemplate.send(
            topic,
            CloudEvent.<TokenManagerCardChanged>builder()
                    .withType(TokenManagerCardChanged.TYPE)
                    .withData(TestUtils.prepareRandomTokenManagerEvent(CardChangeType.INSERT_UPDATE).build())
                    .build()
    );

    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(tkmPaymentInstrumentService, Mockito.atLeast(3)).handle(Mockito.any(TkmUpdateCommand.class));
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
      Mockito.verify(tkmPaymentInstrumentService).handle(captor.capture());

      assertEquals(captor.getValue().getPar(), event.getData().getPar());
      assertEquals(captor.getValue().getHashPan(), event.getData().getHashPan());
      assertEquals(captor.getValue().getTaxCode(), event.getData().getTaxCode());
    });
  }

  @ParameterizedTest
  @ValueSource(classes = {OptimisticLockingFailureException.class, DuplicateKeyException.class})
  void whenRevokeCommandFailWithWriteConflictsThenRetryUntilMaxAttempts(Class<? extends Exception> exception) {
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
      Mockito.verify(tkmPaymentInstrumentService, Mockito.atLeast(3)).handle(Mockito.any(TkmRevokeCommand.class));
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
      Mockito.verify(tkmPaymentInstrumentService, Mockito.atLeast(3)).handle(Mockito.any(TkmRevokeCommand.class));
    });
  }

}