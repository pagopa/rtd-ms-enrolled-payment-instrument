package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.revoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("kafka-test")
@EmbeddedKafka(bootstrapServersProperty = "spring.embedded.kafka.brokers", partitions = 3)
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
@Import({KafkaTestConfiguration.class})
//@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class})
class KafkaRevokeNotificationServiceTest {

  @Value("${test.kafka.topic-rtd-to-app}")
  private String topic;

  @Autowired
  private StreamBridge bridge;
  private KafkaRevokeNotificationService revokeNotificationService;

  private Consumer<String, String> consumer;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp(@Autowired EmbeddedKafkaBroker broker) {
    final var consumerProperties = KafkaTestUtils.consumerProps("group", "true", broker);
    broker.addTopicsWithResults(topic);
    consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProperties).createConsumer();
    consumer.subscribe(List.of(topic));
    revokeNotificationService = new KafkaRevokeNotificationService("rtdToApp-out-0", bridge);
    mapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown(@Autowired EmbeddedKafkaBroker broker) {
    consumer.close();
    broker.doWithAdmin(admin -> admin.deleteTopics(List.of(topic)));
  }

  @Test
  void whenNotifyRevokedCardThenRevokeNotificationProduced() {
    final var typeReference = new TypeReference<CloudEvent<RevokeNotification>>(){};
    final var hashPan = TestUtils.generateRandomHashPan();
    revokeNotificationService.notifyRevoke(Set.of(SourceApp.values()), "taxCode", hashPan);

    await().ignoreException(NoSuchElementException.class).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var records = consumer.poll(Duration.ZERO);
      assertThat(records)
              .map(it -> mapper.readValue(it.value(), typeReference))
              .allMatch(it -> Objects.equals(RevokeNotification.TYPE, it.getType()))
              .map(CloudEvent::getData)
              .allMatch(notification -> Objects.equals("taxCode", notification.getFiscalCode()))
              .allMatch(notification -> Objects.equals(hashPan.getValue(), notification.getHashPan()))
              .allSatisfy(notification -> assertThat(notification.getApplications()).hasSameElementsAs(Set.of(SourceApp.values())));
    });
  }

  @Test
  void whenPublishApplicationInstrumentEventThenShouldBeProducedOnDifferentPartitions() {
    final var hashPans = TestUtils.partitionedHashPans().map(HashPan::create).collect(Collectors.toList());

    hashPans.forEach(it -> revokeNotificationService.notifyRevoke(Set.of(SourceApp.ID_PAY), "taxCode", it));

    await().ignoreException(NoSuchElementException.class).atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      final var records = consumer.poll(Duration.ZERO);
      assertThat(records).hasSize(hashPans.size());
      assertThat(records.partitions()).hasSizeGreaterThan(1);
    });
  }
}