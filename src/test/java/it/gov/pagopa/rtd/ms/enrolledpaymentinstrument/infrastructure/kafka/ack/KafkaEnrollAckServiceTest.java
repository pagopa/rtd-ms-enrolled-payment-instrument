package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.ack;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestKafkaConsumerSetup;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
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
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Date;
import java.util.Objects;
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
class KafkaEnrollAckServiceTest {

  private static final String RTD_TO_APP_BINDING = "rtdToApp-out-0";

  @Value("${test.kafka.topic-rtd-to-app}")
  private String topic;

  @Autowired
  private StreamBridge bridge;
  private KafkaEnrollAckService kafkaEnrollAckService;

  private TestKafkaConsumerSetup.TestConsumer testConsumer;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp(@Autowired EmbeddedKafkaBroker broker) {
    testConsumer = TestKafkaConsumerSetup.setup(broker, topic);
    kafkaEnrollAckService = new KafkaEnrollAckService(bridge, RTD_TO_APP_BINDING);
    mapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown() {
    testConsumer.getContainer().stop();
    testConsumer.getRecords().clear();
  }

  @Test
  @SneakyThrows
  void whenSendEnrollAckThenEnrollEnrollAckEventCloudIsProduced() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var ackTimestamp = new Date();
    final var type = new TypeReference<CloudEvent<EnrollAck>>() {};
    kafkaEnrollAckService.confirmEnroll(hashPan, ackTimestamp);

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var record = testConsumer.getRecords().poll(100, TimeUnit.MILLISECONDS);
      assertThat(record)
              .isNotNull()
              .extracting(TestUtils.parseTo(mapper, type))
              .matches(it -> it.getType().equals(EnrollAck.TYPE))
              .matches(it -> Objects.equals(it.getData().getHashPan(), hashPan.getValue()))
              .matches(it -> Objects.equals(it.getData().getTimestamp(), ackTimestamp));
    });
  }
}