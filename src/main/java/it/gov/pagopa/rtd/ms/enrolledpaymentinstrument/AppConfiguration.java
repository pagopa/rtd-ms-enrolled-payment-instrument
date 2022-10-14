package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.KafkaRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentDao;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentRepositoryImpl;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories")
public class AppConfiguration {

  @Bean
  public EnrolledPaymentInstrumentRepository enrolledPaymentInstrumentRepository(
          EnrolledPaymentInstrumentDao dao
  ) {
    return new EnrolledPaymentInstrumentRepositoryImpl(dao);
  }

  @Bean
  public InstrumentRevokeNotificationService revokeService(StreamBridge bridge) {
    return new KafkaRevokeNotificationService(bridge);
  }

}
