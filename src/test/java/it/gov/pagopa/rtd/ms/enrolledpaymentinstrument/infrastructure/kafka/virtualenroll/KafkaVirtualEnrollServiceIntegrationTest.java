package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.virtualenroll;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestKafkaConsumerSetup;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
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
import java.util.Objects;
import java.util.Set;
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

  private TestKafkaConsumerSetup.TestConsumer testConsumer;
  private ObjectMapper mapper;

  @BeforeEach
  void setUp(@Autowired EmbeddedKafkaBroker broker) {
    testConsumer = TestKafkaConsumerSetup.setup(broker, topic);
    kafkaVirtualEnrollService = new KafkaVirtualEnrollService(RTD_TO_APP_BINDING, bridge);
    mapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown() {
    testConsumer.getContainer().stop();
    testConsumer.getRecords().clear();
  }

  @Test
  @SneakyThrows
  void whenEnrollVirtualCardWithoutHashTokenThenEnrollCardEventCloudWithoutHashTokenIsProduced() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var type = new TypeReference<CloudEvent<VirtualEnroll>>() {};
    kafkaVirtualEnrollService.enroll(hashPan, "12345", Set.of(SourceApp.ID_PAY));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var record = testConsumer.getRecords().poll(100, TimeUnit.MILLISECONDS);
      assertThat(record)
              .isNotNull()
              .extracting(TestUtils.parseTo(mapper, type))
              .matches(it -> it.getType().equals(VirtualEnroll.TYPE))
              .matches(it -> it.getData().getHashPan().equals(hashPan.getValue()))
              .matches(it -> it.getData().getPar().equals("12345"))
              .matches(it -> Objects.isNull(it.getData().getHashToken()))
              .satisfies(it -> assertThat(it.getData().getApplications()).hasSameElementsAs(Set.of(SourceApp.ID_PAY)));
    });
  }

  @Test
  @SneakyThrows
  void whenEnrollVirtualCardWithHashTokenThenEnrollCardEventCloudWithHashTokenIsProduced() {
    final var type = new TypeReference<CloudEvent<VirtualEnroll>>() {};
    final var hashPan = TestUtils.generateRandomHashPan();
    final var hashToken = TestUtils.generateRandomHashPan();
    kafkaVirtualEnrollService.enrollToken(hashPan, hashToken, "12345", Set.of(SourceApp.ID_PAY));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var record = testConsumer.getRecords().poll(100, TimeUnit.MILLISECONDS);
      assertThat(record)
              .isNotNull()
              .extracting(TestUtils.parseTo(mapper, type))
              .matches(it -> it.getType().equals(VirtualEnroll.TYPE))
              .matches(it -> it.getData().getHashPan().equals(hashPan.getValue()))
              .matches(it -> it.getData().getPar().equals("12345"))
              .matches(it -> it.getData().getHashToken().equals(hashToken.getValue()))
              .satisfies(it -> assertThat(it.getData().getApplications()).hasSameElementsAs(Set.of(SourceApp.ID_PAY)));
    });
  }

  @Test
  @SneakyThrows
  void whenUnEnrollVirtualCardThenRevokeTokenEventCloudIsProduced() {
    final var type = new TypeReference<CloudEvent<VirtualRevoke>>() {};
    final var hashPan = TestUtils.generateRandomHashPan();
    final var hashToken = TestUtils.generateRandomHashPan();
    kafkaVirtualEnrollService.unEnrollToken(hashPan, hashToken, "12345", Set.of(SourceApp.ID_PAY));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      final var record = testConsumer.getRecords().poll(100, TimeUnit.MILLISECONDS);
      assertThat(record)
              .isNotNull()
              .extracting(TestUtils.parseTo(mapper, type))
              .matches(it -> it.getType().equals(VirtualRevoke.TYPE))
              .matches(it -> it.getData().getHashPan().equals(hashPan.getValue()))
              .matches(it -> it.getData().getPar().equals("12345"))
              .matches(it -> it.getData().getHashToken().equals(hashToken.getValue()))
              .satisfies(it -> assertThat(it.getData().getApplications()).hasSameElementsAs(Set.of(SourceApp.ID_PAY)));
    });
  }
}
