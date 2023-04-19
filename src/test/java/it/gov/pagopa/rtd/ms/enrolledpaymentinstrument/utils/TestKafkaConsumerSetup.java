package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.Message;
import org.testcontainers.containers.KafkaContainer;

public final class TestKafkaConsumerSetup {

  public record TestConsumer(
      KafkaMessageListenerContainer<String, String> container,
      BlockingQueue<Message<String>> records,
      BlockingQueue<ConsumerRecord<String, String>> consumerRecords
  ) {

  }

  public static TestConsumer setup(KafkaContainer kafkaContainer, String topic, int partitions) {
    final BlockingQueue<Message<String>> records = new LinkedBlockingQueue<>();
    final var consumerRecords = new LinkedBlockingQueue<ConsumerRecord<String, String>>();
    final var consumerProperties = KafkaContainerTestUtils.consumerProps("group", "true",
        kafkaContainer);
    final var containerProperties = new ContainerProperties(topic);
    final KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(
        new DefaultKafkaConsumerFactory<>(consumerProperties), containerProperties
    );
    container.setupMessageListener((MessageListener<String, String>) record -> {
      records.add((Message<String>) new MessagingMessageConverter().toMessage(record, null, null,
          String.class));
      consumerRecords.add(record);
    });
    final var testConsumer = new TestConsumer(container, records, consumerRecords);
    testConsumer.container.start();
    ContainerTestUtils.waitForAssignment(container, partitions);
    return testConsumer;
  }

}
