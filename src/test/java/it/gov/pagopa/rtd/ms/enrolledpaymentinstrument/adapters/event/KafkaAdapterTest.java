package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.SomethingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
public class KafkaAdapterTest {
  @Autowired
  private StreamBridge stream;

  @SpyBean
  SomethingService somethingService;

  @Test
  public void consumeAnEvent() throws InterruptedException {
    final var message = MessageBuilder.withPayload("ciao").build();
    final var isSent = stream.send("enrolledPaymentInstrumentConsumer-in-0", message);

    assertTrue(isSent);
    Mockito.verify(somethingService).processSomething("ciao");
  }

}