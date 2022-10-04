package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = {"test.kafka.topic"},
        partitions = 1,
        bootstrapServersProperty = "spring.embedded.kafka.brokers"
)
@TestPropertySource(properties = {"spring.config.location=classpath:application-test.yml"}, inheritProperties = false)
@Import(value = {KafkaEnrolledInstrumentEventsAdapter.class, KafkaEnrolledInstrumentEventsAdapterTest.MockConfiguration.class})
@EnableAutoConfiguration(exclude = {EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
class KafkaTkmTokenUpdateEventsAdapterTest {

    private static final String BINDING_NAME = "tkmTokenUpdateConsumer-in-0";

    @Autowired
    private StreamBridge streamBridge;

    @Test
    void dummyTest() {
        streamBridge.send(BINDING_NAME, "{\"data\": \"value\"}");
        assertTrue(true);
    }

}