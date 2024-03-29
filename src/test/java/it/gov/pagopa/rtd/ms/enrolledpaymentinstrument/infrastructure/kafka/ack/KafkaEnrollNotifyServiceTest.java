package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.ack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.CorrelationIdService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.KafkaContainerTestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.TestKafkaConsumerSetup;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.TestUtils;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Import({TestChannelBinderConfiguration.class, KafkaTestConfiguration.class})
class KafkaEnrollNotifyServiceTest {

  private static final String RTD_TO_APP_BINDING = "rtdToApp-out-0";

  @Autowired
  private CorrelationIdService correlationIdService;
  private KafkaEnrollNotifyService kafkaEnrollNotifyService;

  @Autowired
  private OutputDestination outputDestination;

  private ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp(@Autowired StreamBridge streamBridge) {
    kafkaEnrollNotifyService = new KafkaEnrollNotifyService(streamBridge, RTD_TO_APP_BINDING, correlationIdService);
  }

  @Test
  @SneakyThrows
  void whenSendEnrollAckThenEnrollEnrollAckEventCloudIsProduced() {
    final var typeReference = new TypeReference<CloudEvent<EnrollAck>>() {};
    final var hashPan = TestUtils.generateRandomHashPan();
    final var ackTimestamp = new Date();
    correlationIdService.setCorrelationId("1234");
    kafkaEnrollNotifyService.confirmEnroll(SourceApp.ID_PAY, hashPan, ackTimestamp);

    final var sent = objectMapper.readValue(outputDestination.receive().getPayload(), typeReference);

    assertThat(sent)
        .isNotNull()
        .matches(it -> it.getType().equals(EnrollAck.TYPE))
        .matches(it -> Objects.equals(it.getData().getHashPan(), hashPan.getValue()))
        .matches(it -> Objects.equals(it.getData().getTimestamp(), ackTimestamp))
        .matches(it -> Objects.equals(it.getData().getApplication(), SourceApp.ID_PAY))
        .matches(it -> Objects.equals(it.getCorrelationId(), "1234"));
  }

    @Test
  @SneakyThrows
  void whenSendExportConfirmThenPaymentInstrumentExportedEventCloudIsProduced() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var timestamp = new Date();
    final var type = new TypeReference<CloudEvent<PaymentInstrumentExported>>() {};
    kafkaEnrollNotifyService.confirmExport(hashPan, timestamp);

    assertThat(objectMapper.readValue(outputDestination.receive().getPayload(), type))
        .isNotNull()
        .matches(it -> it.getType().equals(PaymentInstrumentExported.TYPE))
        .matches(it -> Objects.equals(it.getData().getHashPan(), hashPan.getValue()))
        .matches(it -> Objects.equals(it.getData().getTimestamp(), timestamp))
        .matches(it -> Objects.isNull(it.getCorrelationId()));
  }

  @Nested
  @SpringBootTest
  @ActiveProfiles("kafka-test")
  @Testcontainers
  @EnableAutoConfiguration
  class KafkaIntegrationPartitionTest {
    @Container
    public static final KafkaContainer kafkaContainer = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))
        .withEmbeddedZookeeper();

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
      registry.add("spring.cloud.stream.kafka.binder.brokers", kafkaContainer::getBootstrapServers);
      registry.add("test.broker", kafkaContainer::getBootstrapServers);
      registry.add("test.partitionCount", () -> 3);
    }

    @Value("${test.kafka.topic-rtd-to-app}")
    private String topic;

    private AdminClient adminClient;
    private TestKafkaConsumerSetup.TestConsumer testConsumer;

    @BeforeEach
    void setUp(@Autowired StreamBridge streamBridge) throws ExecutionException, InterruptedException {
      adminClient = KafkaContainerTestUtils.createAdminClient(kafkaContainer);
      adminClient.createTopics(List.of(new NewTopic(topic, 3, (short) 1))).all().get();
      testConsumer = TestKafkaConsumerSetup.setup(kafkaContainer, topic, 3);
      kafkaEnrollNotifyService = new KafkaEnrollNotifyService(streamBridge, RTD_TO_APP_BINDING, correlationIdService);
    }

    @AfterEach
    void tearDown() {
      testConsumer.container().stop();
      testConsumer.records().clear();
      adminClient.deleteTopics(List.of(topic));
    }

    @Test
    void whenPublishApplicationInstrumentEventThenShouldBeProducedOnDifferentPartitions() {
      final var hashPans = TestUtils.partitionedHashPans().map(HashPan::create)
          .toList();

      hashPans.forEach(
          it -> kafkaEnrollNotifyService.confirmEnroll(SourceApp.ID_PAY, it, new Date()));

      await().ignoreException(NoSuchElementException.class).atMost(Duration.ofSeconds(15))
          .untilAsserted(() -> {
            final var records = testConsumer.consumerRecords();
            final var partitions = records.stream()
                .collect(Collectors.groupingBy(ConsumerRecord::partition));
            assertThat(records).hasSize(hashPans.size());
            assertThat(partitions).hasSizeGreaterThan(1);
          });
    }
  }
}