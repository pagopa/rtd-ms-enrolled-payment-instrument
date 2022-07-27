package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model")
public class MongoConfig {

  @Bean
  MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
    return new MongoTransactionManager(dbFactory);
  }
}
