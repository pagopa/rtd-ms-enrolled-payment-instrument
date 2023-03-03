package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.ExportCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.KafkaConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.PaymentInstrumentExported;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.test.binder.TestSupportBinderAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("kafka-test")
@EmbeddedKafka(bootstrapServersProperty = "spring.embedded.kafka.brokers")
@Import({ExportEventAdapter.class, KafkaTestConfiguration.class, KafkaConfiguration.class})
@ImportAutoConfiguration(ValidationAutoConfiguration.class)
@EnableAutoConfiguration(exclude = {TestSupportBinderAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class})
class ExportEventAdapterTest {

  private static final int DEFAULT_AT_MOST_TIMEOUT = 10; // seconds

  @Value("${test.kafka.topic}")
  private String topic;

  @Autowired
  private EnrolledPaymentInstrumentService paymentInstrumentService;

  private KafkaTemplate<String, CloudEvent<PaymentInstrumentExported>> kafkaTemplate;

  @BeforeEach
  void setup(@Autowired EmbeddedKafkaBroker broker) {
    Mockito.reset(paymentInstrumentService);
    kafkaTemplate = new KafkaTemplate<>(
            new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(broker), new StringSerializer(), new JsonSerializer<>())
    );
    broker.addTopicsWithResults(topic);
  }

  @AfterEach
  void teardown(@Autowired EmbeddedKafkaBroker broker) {
    kafkaTemplate.destroy();
    broker.doWithAdmin(admin -> admin.deleteTopics(List.of(topic)));
  }

  @Test
  void whenExportEventThenExecuteValidExportCommand() {
    final var captor = ArgumentCaptor.forClass(ExportCommand.class);
    final var event = CloudEvent.<PaymentInstrumentExported>builder()
            .withType(PaymentInstrumentExported.TYPE)
            .withData(new PaymentInstrumentExported(TestUtils.generateRandomHashPanAsString()))
            .build();
    kafkaTemplate.send(topic, event);
    await().atMost(Duration.ofSeconds(DEFAULT_AT_MOST_TIMEOUT)).untilAsserted(() -> {
      Mockito.verify(paymentInstrumentService).handle(captor.capture());

      assertThat(captor.getValue().getHashPan()).isEqualTo(event.getData().getPaymentInstrumentId());
      assertThat(captor.getValue().getExportedAt()).isNotNull();
    });
  }

  @Test
  void whenExportEventWithMissingMandatoryFieldsThenAdapterShouldNotCallService() {
    final var event = CloudEvent.<PaymentInstrumentExported>builder()
            .withType(TokenManagerCardChanged.TYPE)
            .withData(new PaymentInstrumentExported(null))
            .build();
    kafkaTemplate.send(topic, event);

    await().during(Duration.ofSeconds(3)).untilAsserted(() -> {
      Mockito.verify(paymentInstrumentService, Mockito.times(0)).handle(Mockito.any(ExportCommand.class));
    });
  }
}