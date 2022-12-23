package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentDao;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories")
public class RepositoryConfiguration {
  @Bean
  public EnrolledPaymentInstrumentRepository enrolledPaymentInstrumentRepository(
          EnrolledPaymentInstrumentDao dao
  ) {
    return new EnrolledPaymentInstrumentRepositoryImpl(dao);
  }
}
