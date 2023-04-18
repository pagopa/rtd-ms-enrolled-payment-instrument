package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils;

import io.vavr.control.Try;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testcontainers.containers.KafkaContainer;

public class KafkaContainerTestUtils {

  public static AdminClient createAdminClient(KafkaContainer kafkaContainer) {
    return AdminClient.create(Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()));
  }

  public static void createTopic(KafkaContainer kafkaContainer, String topic, int partitions) {
    Try.of(() -> createAdminClient(kafkaContainer)
        .createTopics(List.of(new NewTopic(topic, partitions, (short) 1)))
        .all()
        .get());
  }

  public static Map<String, Object> consumerProps(String group, String autoCommit, KafkaContainer kafkaContainer) {
    Map<String, Object> props = new HashMap<>();
    props.put("bootstrap.servers", kafkaContainer.getBootstrapServers());
    props.put("group.id", group);
    props.put("enable.auto.commit", autoCommit);
    props.put("auto.commit.interval.ms", "10");
    props.put("session.timeout.ms", "60000");
    props.put("key.deserializer", IntegerDeserializer.class);
    props.put("value.deserializer", StringDeserializer.class);
    props.put("auto.offset.reset", "earliest");
    return props;
  }

  public static Map<String, Object> producerProps(KafkaContainer kafkaContainer) {
    Map<String, Object> props = new HashMap<>();
    props.put("bootstrap.servers", kafkaContainer.getBootstrapServers());
    props.put("batch.size", "16384");
    props.put("linger.ms", 1);
    props.put("buffer.memory", "33554432");
    props.put("key.serializer", IntegerSerializer .class);
    props.put("value.serializer", StringSerializer .class);
    return props;
  }



}
