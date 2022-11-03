package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;

import lombok.Data;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class TestKafkaConsumerSetup {

  @Data
  public static final class TestConsumer {
    private final KafkaMessageListenerContainer<String, String> container;
    private final BlockingQueue<Message<String>> records;
  }

  public static TestConsumer setup(EmbeddedKafkaBroker broker, String topic) {
    final BlockingQueue<Message<String>> records = new LinkedBlockingQueue<>();
    final var consumerProperties = KafkaTestUtils.consumerProps("group", "true", broker);
    final var containerProperties = new ContainerProperties(topic);
    final KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(
            new DefaultKafkaConsumerFactory<>(consumerProperties), containerProperties
    );
    container.setupMessageListener((MessageListener<String, String>) record -> {
      records.add((Message<String>) new MessagingMessageConverter().toMessage(record, null, null, String.class));
    });
    final var testConsumer = new TestConsumer(container, records);
    testConsumer.container.start();
    ContainerTestUtils.waitForAssignment(container, broker.getPartitionsPerTopic());
    return testConsumer;
  }

}
