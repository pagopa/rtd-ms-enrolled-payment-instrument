package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.config;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentDao;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import javax.validation.Validation;
import javax.validation.Validator;

public class KafkaTestConfiguration {
  @MockBean
  private EnrolledPaymentInstrumentDao dao;
  @MockBean
  private EnrolledPaymentInstrumentRepository repository;
  @MockBean
  private EnrolledPaymentInstrumentService paymentInstrumentService;
  @Bean
  Validator validator() {
    try (final var factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }
}
