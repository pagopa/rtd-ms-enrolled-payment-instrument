package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = {"${test.kafka.topic-revoke}"},
        partitions = 1,
        bootstrapServersProperty = "spring.embedded.kafka.brokers"
)
@Import({KafkaTestConfiguration.class})
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ExtendWith(MockitoExtension.class)
class KafkaRevokeNotificationServiceTest {

  @Value("${test.kafka.topic-revoke}")
  private String topic;

  @Autowired
  private StreamBridge bridge;
  private KafkaRevokeNotificationService revokeNotificationService;

  private Consumer<String, String> consumer;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp(@Autowired EmbeddedKafkaBroker broker) {
    final var consumerProperties = KafkaTestUtils.consumerProps("group", "true", broker);
    consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProperties).createConsumer();
    consumer.subscribe(List.of(topic));
    revokeNotificationService = new KafkaRevokeNotificationService(bridge);
    mapper =  new ObjectMapper();
  }

  @AfterEach
  void tearDown() {
    consumer.close();
  }

  @Test
  void whenNotifyRevokedCardThenRevokeNotificationProduced() {
    final var hashPan = TestUtils.generateRandomHashPan();
    revokeNotificationService.notifyRevoke("taxCode", hashPan);

    await().ignoreException(NoSuchElementException.class).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var records = consumer.poll(Duration.ZERO);
      final var notification = mapper.readValue(records.iterator().next().value(), RevokeNotification.class);

      assertEquals("taxCode", notification.getFiscalCode());
      assertEquals(hashPan.getValue(), notification.getHashPan());
    });
  }
}