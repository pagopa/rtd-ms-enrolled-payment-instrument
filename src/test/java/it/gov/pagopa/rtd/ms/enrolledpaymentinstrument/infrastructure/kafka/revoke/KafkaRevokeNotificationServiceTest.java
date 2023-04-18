package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.revoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.KafkaContainerTestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(SpringExtension.class)
@Import({KafkaTestConfiguration.class})
class KafkaRevokeNotificationServiceTest {

  private StreamBridge streamBridge;
  private KafkaRevokeNotificationService revokeNotificationService;

  @BeforeEach
  void setUp() {
    streamBridge = mock(StreamBridge.class);
    revokeNotificationService = new KafkaRevokeNotificationService("rtdToApp-out-0", streamBridge);
  }

  @AfterEach
  void tearDown() {
    reset(streamBridge);
  }

  @Test
  void whenNotifyRevokedCardThenRevokeNotificationProduced() {
    final var messageCaptor = ArgumentCaptor.forClass(Message.class);
    final var hashPan = TestUtils.generateRandomHashPan();
    revokeNotificationService.notifyRevoke(Set.of(SourceApp.values()), "taxCode", hashPan);

    verify(streamBridge, times(1)).send(any(), messageCaptor.capture());

    assertThat(messageCaptor.getAllValues())
        .map(it -> (CloudEvent<RevokeNotification>) it.getPayload())
        .allMatch(it -> Objects.equals(RevokeNotification.TYPE, it.getType()))
        .map(CloudEvent::getData)
        .allMatch(notification -> Objects.equals("taxCode", notification.getFiscalCode()))
        .allMatch(notification -> Objects.equals(hashPan.getValue(), notification.getHashPan()))
        .allSatisfy(notification -> assertThat(notification.getApplications()).hasSameElementsAs(
            Set.of(SourceApp.values())));
  }

  @Nested
  @SpringBootTest
  @ActiveProfiles("kafka-test")
  @Testcontainers
  @EnableAutoConfiguration
  class KafkaIntegrationPartionTest {

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
    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp(@Autowired StreamBridge streamBridge) throws ExecutionException, InterruptedException {
      adminClient = KafkaContainerTestUtils.createAdminClient(kafkaContainer);
      adminClient.createTopics(List.of(new NewTopic(topic, 3, (short) 1))).all().get();
      consumer = new DefaultKafkaConsumerFactory<String, String>(
          KafkaContainerTestUtils.consumerProps("group", "true", kafkaContainer)
      ).createConsumer();
      consumer.subscribe(List.of(topic));
      revokeNotificationService = new KafkaRevokeNotificationService("rtdToApp-out-0", streamBridge);
    }

    @AfterEach
    void tearDown() {
      consumer.close();
      adminClient.deleteTopics(List.of(topic));
    }

    @Test
    void whenPublishApplicationInstrumentEventThenShouldBeProducedOnDifferentPartitions() {
      final var hashPans = TestUtils.partitionedHashPans().map(HashPan::create).toList();

      hashPans.forEach(it -> revokeNotificationService.notifyRevoke(Set.of(SourceApp.ID_PAY), "taxCode", it));

      await().ignoreException(NoSuchElementException.class).atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
        final var records = consumer.poll(Duration.ZERO);
        assertThat(records).hasSize(hashPans.size());
        assertThat(records.partitions()).hasSizeGreaterThan(1);
      });
    }
  }
}