package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@TestConfiguration
@Testcontainers
public class MongodbIntegrationTestConfiguration {

  @Container
  public static final MongoDBContainer mongoContainer = new MongoDBContainer("mongo:4.4.4");

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
  }
}
