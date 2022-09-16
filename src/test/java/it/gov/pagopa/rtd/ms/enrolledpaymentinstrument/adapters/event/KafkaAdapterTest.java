package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.adapters.event;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentDao;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    topics = { "test.kafka.topic" },
    partitions = 1,
    bootstrapServersProperty = "spring.embedded.kafka.brokers"
)
@TestPropertySource(properties = { "spring.config.location=classpath:application-test.yml" }, inheritProperties = false)
@Import(value = { KafkaAdapter.class, KafkaAdapterTest.MockConfiguration.class })
@EnableAutoConfiguration(exclude = {EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
class KafkaAdapterTest {

  private static final String BINDING_NAME = "enrolledPaymentInstrumentConsumer-in-0";

  @Autowired
  private StreamBridge stream;

  @MockBean
  EnrolledPaymentInstrumentService paymentInstrumentService;

  @Test
  void shouldCreateApplicationCommandBasedOnEvent() {
    final var captor = ArgumentCaptor.forClass(EnrollPaymentInstrumentCommand.class);
    final var message = MessageBuilder.withPayload(enabledPaymentInstrumentEvent).build();
    final var isSent = stream.send(BINDING_NAME, message);

    assertTrue(isSent);
    Mockito.verify(paymentInstrumentService).handle(captor.capture());

    assertEquals(hashPanEvent, captor.getValue().getHashPan());
    assertEquals(sourceAppEvent, captor.getValue().getSourceApp());
    assertEquals(Operation.CREATE, captor.getValue().getOperation());
  }

  @Test
  void shouldFailToCreateCommandWithMalformedEvent() {
    final var message = MessageBuilder.withPayload(enabledPaymentInstrumentEvent.replace("CREATE", "123")).build();

    final var exception = assertThrows(MessageHandlingException.class, () -> stream.send(BINDING_NAME, message));

    assertTrue(exception.getCause() instanceof IllegalArgumentException);
  }

  private static final String hashPanEvent = "42771c850db05733b749d7e05153d0b8c77b54949d99740343696bc483a07aba";
  private static final String sourceAppEvent = "FA";
  private static final String enabledPaymentInstrumentEvent = ""
      + "{\n"
      + "  \"hashPan\": \"" + hashPanEvent + "\",\n"
      + "  \"app\": \"" + sourceAppEvent + "\",\n"
      + "  \"operation\": \"CREATE\"\n"
      + "}";

  @Configuration
  static class MockConfiguration {
    @MockBean
    private EnrolledPaymentInstrumentDao dao;
    @MockBean
    private EnrolledPaymentInstrumentRepository repository;
  }
}