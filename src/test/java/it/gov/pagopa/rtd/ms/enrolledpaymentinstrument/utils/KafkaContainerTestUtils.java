package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.containers.KafkaContainer;

public class KafkaContainerTestUtils {

  public static AdminClient createAdminClient(KafkaContainer kafkaContainer) {
    return AdminClient.create(Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers()));
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

}
