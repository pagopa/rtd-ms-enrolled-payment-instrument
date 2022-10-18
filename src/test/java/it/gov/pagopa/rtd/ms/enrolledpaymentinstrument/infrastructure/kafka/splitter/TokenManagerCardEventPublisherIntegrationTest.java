package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.CardChangeType;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = {"${test.kafka.topic-tkm}"},
        partitions = 2,
        bootstrapServersProperty = "spring.embedded.kafka.brokers"
)
@Import({KafkaTestConfiguration.class})
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ExtendWith(MockitoExtension.class)
class TokenManagerCardEventPublisherIntegrationTest {

  @Value("${test.kafka.topic-tkm}")
  private String topic;

  @Autowired
  private StreamBridge bridge;
  private TokenManagerCardEventPublisher cardEventPublisher;

  private Consumer<String, String> consumer;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setup(@Autowired EmbeddedKafkaBroker broker) {
    final var consumerProperties = KafkaTestUtils.consumerProps("group", "true", broker);
    consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProperties).createConsumer();
    consumer.subscribe(List.of(topic));
    cardEventPublisher = new TokenManagerCardEventPublisher("cardChangedProducer-out-0", bridge);
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown() {
    consumer.close();
  }

  @Test
  void whenPublishCardChangedEventThenShouldBeProducedOnDifferentPartitions() {
    final var events = IntStream.range(0, 10)
            .mapToObj(i -> TestUtils.prepareRandomTokenManagerEvent(CardChangeType.UPDATE).build());

    events.forEach(it -> cardEventPublisher.sendTokenManagerCardChanged(it));

    await().ignoreException(NoSuchElementException.class).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var records = consumer.poll(Duration.ZERO);
      assertThat(records).hasSize(10);
      assertThat(records.partitions()).hasSize(2);
    });
  }

  @Test
  void whenPublishCardChangedEventThenMustHaveSamePayload() {
    final var events = IntStream.range(0, 10)
            .mapToObj(i -> TestUtils.prepareRandomTokenManagerEvent(CardChangeType.UPDATE).build())
            .collect(Collectors.toList());

    events.forEach(it -> cardEventPublisher.sendTokenManagerCardChanged(it));

    await().ignoreException(NoSuchElementException.class).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var records = consumer.poll(Duration.ZERO);
      assertThat(records).map(it -> objectMapper.readValue(it.value(), TokenManagerCardChanged.class))
              .hasSameElementsAs(events);
    });
  }
}