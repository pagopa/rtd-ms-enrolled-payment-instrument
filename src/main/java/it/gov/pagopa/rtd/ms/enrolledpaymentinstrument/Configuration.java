package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model.EnrolledPaymentInstrumentDao;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

@org.springframework.context.annotation.Configuration
public class Configuration {

  @Bean
  public EnrolledPaymentInstrumentRepository enrolledPaymentInstrumentRepository(
      EnrolledPaymentInstrumentDao dao,
      MongoTemplate mongoTemplate
  ) {
    return new EnrolledPaymentInstrumentRepositoryImpl(dao, mongoTemplate);
  }

}
