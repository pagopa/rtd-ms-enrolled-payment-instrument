package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    topics = { "test.kafka.topic" },
    partitions = 1,
    bootstrapServersProperty = "spring.embedded.kafka.brokers"
)
@TestPropertySource(properties = { "spring.config.location=classpath:application-test.yml" }, inheritProperties = false)
@Import(KafkaAdapter.class)
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class }) // exclude jpa initialization
class KafkaAdapterTest {
  @Autowired
  private StreamBridge stream;

  @MockBean
  EnrolledPaymentInstrumentService somethingService;

  @Test
  void consumeAnEvent() {
    final var captor = ArgumentCaptor.forClass(EnrollPaymentInstrumentCommand.class);
    final var message = MessageBuilder.withPayload(enabledPaymentInstrumentEvent).build();
    final var isSent = stream.send("enrolledPaymentInstrumentConsumer-in-0", message);

    assertTrue(isSent);
    Mockito.verify(somethingService).handle(captor.capture());

    assertEquals(hashPanEvent, captor.getValue().getHashPan());
    assertEquals(sourceAppEvent, captor.getValue().getSourceApp());
  }

  private static final String hashPanEvent = "42771c850db05733b749d7e05153d0b8c77b54949d99740343696bc483a07aba";
  private static final String sourceAppEvent = "FA";
  private static final String enabledPaymentInstrumentEvent = ""
      + "{\n"
      + "  \"hashPan\": \"" + hashPanEvent + "\",\n"
      + "  \"app\": \"" + sourceAppEvent + "\",\n"
      + "  \"enable\": true\n"
      + "}";

}