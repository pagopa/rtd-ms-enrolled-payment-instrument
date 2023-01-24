package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.ack;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestKafkaConsumerSetup;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.CorrelationIdService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("kafka-test")
@EmbeddedKafka(bootstrapServersProperty = "spring.embedded.kafka.brokers")
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
@Import({KafkaTestConfiguration.class})
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class})
class KafkaEnrollAckServiceTest {

  private static final String RTD_TO_APP_BINDING = "rtdToApp-out-0";

  @Value("${test.kafka.topic-rtd-to-app}")
  private String topic;

  @Autowired
  private StreamBridge bridge;
  @Autowired
  private CorrelationIdService correlationIdService;
  private KafkaEnrollAckService kafkaEnrollAckService;

  private TestKafkaConsumerSetup.TestConsumer testConsumer;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp(@Autowired EmbeddedKafkaBroker broker) {
    broker.addTopicsWithResults(topic);
    testConsumer = TestKafkaConsumerSetup.setup(broker, topic);
    kafkaEnrollAckService = new KafkaEnrollAckService(bridge, RTD_TO_APP_BINDING, correlationIdService);
    mapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown(@Autowired EmbeddedKafkaBroker broker) {
    testConsumer.getContainer().stop();
    testConsumer.getRecords().clear();
    broker.doWithAdmin(admin -> admin.deleteTopics(List.of(topic)));
  }

  @Test
  @SneakyThrows
  void whenSendEnrollAckThenEnrollEnrollAckEventCloudIsProduced() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var ackTimestamp = new Date();
    final var type = new TypeReference<CloudEvent<EnrollAck>>() {};
    correlationIdService.setCorrelationId("1234");
    kafkaEnrollAckService.confirmEnroll(SourceApp.ID_PAY, hashPan, ackTimestamp);

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var record = testConsumer.getRecords().poll(100, TimeUnit.MILLISECONDS);
      assertThat(record)
              .isNotNull()
              .extracting(TestUtils.parseTo(mapper, type))
              .matches(it -> it.getType().equals(EnrollAck.TYPE))
              .matches(it -> Objects.equals(it.getData().getHashPan(), hashPan.getValue()))
              .matches(it -> Objects.equals(it.getData().getTimestamp(), ackTimestamp))
              .matches(it -> Objects.equals(it.getData().getApplication(), SourceApp.ID_PAY))
              .matches(it -> Objects.equals(it.getCorrelationId(), "1234"));
    });
  }
}