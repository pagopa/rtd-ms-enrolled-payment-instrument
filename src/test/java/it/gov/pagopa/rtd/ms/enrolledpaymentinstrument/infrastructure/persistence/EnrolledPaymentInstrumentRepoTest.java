package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model.EnrolledPaymentInstrumentDao;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentRepositoryImpl;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;


@DataMongoTest
@EntityScan("it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model")
@Import(TestBeanConfiguration.class)
public class EnrolledPaymentInstrumentRepoTest {

  @Autowired
  private EnrolledPaymentInstrumentRepositoryImpl repository;

  @BeforeAll
  static void setup() {

  }

  @Test
  void shouldSaveAnEntity() throws ExecutionException, InterruptedException {
    final var domainObject = new EnrolledPaymentInstrument("ciao", "ciao2");
    final var id = repository.save(domainObject).get();
    final var foundDomainObject = repository.findById(id).get();
    assertNotNull(id);
    assertEquals(foundDomainObject.getSomething1(), domainObject.getSomething1());
  }

}

@TestConfiguration
class TestBeanConfiguration {

  @Autowired
  private EnrolledPaymentInstrumentDao dao;

  @Bean
  public EnrolledPaymentInstrumentRepositoryImpl enrolledPaymentInstrumentRepository() {
    return new EnrolledPaymentInstrumentRepositoryImpl(dao);
  }

}