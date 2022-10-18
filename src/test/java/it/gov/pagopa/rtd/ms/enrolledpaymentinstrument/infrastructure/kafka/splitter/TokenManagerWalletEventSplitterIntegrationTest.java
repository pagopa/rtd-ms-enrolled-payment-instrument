package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.KafkaConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.SplitterConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerWalletChanged;
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
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.integration.handler.GenericHandler;
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
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = {"${test.kafka.topic-wallet-tkm}"},
        partitions = 1,
        bootstrapServersProperty = "spring.embedded.kafka.brokers"
)
@TestPropertySource(properties = {"spring.config.location=classpath:application-test.yml"}, inheritProperties = false)
@Import(value = {KafkaTestConfiguration.class, KafkaConfiguration.class, SplitterConfiguration.class})
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class})
class TokenManagerWalletEventSplitterIntegrationTest {

  @Value("${test.kafka.topic-wallet-tkm}")
  private String topic;

  @MockBean
  private Function<TokenManagerWalletChanged, List<TokenManagerCardChanged>> splitter;

  @MockBean
  private GenericHandler<TokenManagerCardChanged> cardEventPublisher;

  private KafkaTemplate<String, TokenManagerWalletChanged> kafkaTemplate;

  @BeforeEach
  void setup(@Autowired EmbeddedKafkaBroker broker) {
    kafkaTemplate = new KafkaTemplate<>(
            new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(broker), new StringSerializer(), new JsonSerializer<>())
    );
  }

  @AfterEach
  void teardown() {
    kafkaTemplate.destroy();
    Mockito.reset(cardEventPublisher, splitter);
  }

  @ParameterizedTest
  @ValueSource(classes = {OptimisticLockingFailureException.class})
  void whenFailToPublishSplitEventsThenRetryWholeSplitting(Class<? extends Exception> exception) {
    Mockito.doThrow(exception)
            .when(cardEventPublisher)
            .handle(Mockito.any(), Mockito.any());

    Mockito.doReturn(List.of(
            new TokenManagerCardChanged("hpan", "taxCode", "par", List.of(), CardChangeType.UPDATE))
    ).when(splitter).apply(Mockito.any());

    final var walletChanged = new TokenManagerWalletChanged("taxCode", new Date(), List.of(
            new TokenManagerWalletChanged.CardItem("hpan", "par", CardChangeType.UPDATE, null)
    ));

    kafkaTemplate.send(topic, walletChanged);

    await().pollDelay(Duration.ofSeconds(10)).atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      Mockito.verify(cardEventPublisher, Mockito.atLeast(3)).handle(Mockito.any(), Mockito.any());
    });
  }

  @ParameterizedTest
  @ValueSource(classes = {IllegalArgumentException.class})
  void whenFailToSplitDueToInvalidPayloadThenNoRetryHappens(Class<? extends Exception> exception) {
    final var walletChanged = new TokenManagerWalletChanged("taxCode", new Date(), List.of(
            new TokenManagerWalletChanged.CardItem("hpan", "par", CardChangeType.UPDATE, null)
    ));

    Mockito.doThrow(exception).when(splitter).apply(Mockito.any());
    kafkaTemplate.send(topic, walletChanged);
    await().during(Duration.ofSeconds(5)).untilAsserted(() -> {
      Mockito.verify(splitter, Mockito.times(1)).apply(Mockito.any());
    });
  }

  @Test
  void whenReceiveWalletChangeEventThenSplitAndPublish() {
    final ArgumentCaptor<TokenManagerCardChanged> captor = ArgumentCaptor.forClass(TokenManagerCardChanged.class);
    final var walletChanged = new TokenManagerWalletChanged("taxCode", new Date(), List.of());
    Mockito.doReturn(List.of(
            new TokenManagerCardChanged("hpan", "taxCode", "par", List.of(), CardChangeType.UPDATE),
            new TokenManagerCardChanged("hpan2", "taxCode", "par2", List.of(), CardChangeType.UPDATE))
    ).when(splitter).apply(Mockito.any());


    Mockito.doReturn(null).when(cardEventPublisher).handle(Mockito.any(), Mockito.any());
    kafkaTemplate.send(topic, walletChanged);

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      Mockito.verify(cardEventPublisher, Mockito.times(2)).handle(captor.capture(), Mockito.any());
      assertThat(captor.getAllValues()).hasSize(2);
    });
  }
}