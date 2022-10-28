package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.virtualenroll;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import lombok.SneakyThrows;
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
import org.springframework.cloud.function.cloudevent.CloudEventMessageUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@EmbeddedKafka(topics = {"${test.kafka.topic-rtd-to-app}"}, partitions = 1, bootstrapServersProperty = "spring.embedded.kafka.brokers")
@Import({KafkaTestConfiguration.class})
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ExtendWith(MockitoExtension.class)
class KafkaVirtualEnrollServiceIntegrationTest {

  private static final String RTD_TO_APP_BINDING = "rtdToApp-out-0";

  @Value("${test.kafka.topic-rtd-to-app}")
  private String topic;

  @Autowired
  private StreamBridge bridge;
  private KafkaVirtualEnrollService kafkaVirtualEnrollService;

  private KafkaMessageListenerContainer<String, String> container;
  private BlockingQueue<Message<String>> records;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp(@Autowired EmbeddedKafkaBroker broker) {
    final var consumerProperties = KafkaTestUtils.consumerProps("group", "true", broker);
    final var containerProperties = new ContainerProperties(topic);
    records = new LinkedBlockingQueue<>();
    container = new KafkaMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(consumerProperties), containerProperties);
    container.setupMessageListener((MessageListener<String, String>) record -> {
      records.add((Message<String>) new MessagingMessageConverter().toMessage(record, null, null, String.class));
    });
    kafkaVirtualEnrollService = new KafkaVirtualEnrollService(RTD_TO_APP_BINDING, bridge);
    mapper = new ObjectMapper();
    container.start();
    ContainerTestUtils.waitForAssignment(container, broker.getPartitionsPerTopic());
    System.out.println("assignedddddd: " + container.getAssignedPartitions());
  }

  @AfterEach
  void tearDown() {
    container.stop();
    records.clear();
  }

  @Test
  @SneakyThrows
  void whenEnrollVirtualCardWithoutHashTokenThenEnrollCardEventCloudWithoutHashTokenIsProduced() {
    final var hashPan = TestUtils.generateRandomHashPan();
    kafkaVirtualEnrollService.enroll(hashPan, "12345");

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var record = records.poll(100, TimeUnit.MILLISECONDS);
      assertThat(record)
              .isNotNull()
              .matches(it -> CloudEventMessageUtils.getType(it).equals(VirtualEnroll.TYPE))
              .extracting(TestUtils.parseTo(mapper, VirtualEnroll.class))
              .matches(it -> it.getHashPan().equals(hashPan.getValue()))
              .matches(it -> it.getPar().equals("12345"))
              .matches(it -> Objects.isNull(it.getHashToken()));
    });
  }

  @Test
  @SneakyThrows
  void whenEnrollVirtualCardWithHashTokenThenEnrollCardEventCloudWithHashTokenIsProduced() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var hashToken = TestUtils.generateRandomHashPan();
    kafkaVirtualEnrollService.enroll(hashPan, hashToken, "12345");

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var record = records.poll(100, TimeUnit.MILLISECONDS);
      assertThat(record)
              .isNotNull()
              .matches(it -> CloudEventMessageUtils.getType(it).equals(VirtualEnroll.TYPE))
              .extracting(TestUtils.parseTo(mapper, VirtualEnroll.class))
              .matches(it -> it.getHashPan().equals(hashPan.getValue()))
              .matches(it -> it.getPar().equals("12345"))
              .matches(it -> it.getHashToken().equals(hashToken.getValue()));
    });
  }

  @Test
  @SneakyThrows
  void whenUnEnrollVirtualCardThenRevokeTokenEventCloudIsProduced() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var hashToken = TestUtils.generateRandomHashPan();
    kafkaVirtualEnrollService.unEnroll(hashPan, hashToken, "12345");

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var record = records.poll(100, TimeUnit.MILLISECONDS);
      assertThat(record)
              .isNotNull()
              .matches(it -> CloudEventMessageUtils.getType(it).equals(VirtualRevoke.TYPE))
              .extracting(TestUtils.parseTo(mapper, VirtualRevoke.class))
              .matches(it -> it.getHashPan().equals(hashPan.getValue()))
              .matches(it -> it.getPar().equals("12345"))
              .matches(it -> it.getHashToken().equals(hashToken.getValue()));
    });
  }
}
