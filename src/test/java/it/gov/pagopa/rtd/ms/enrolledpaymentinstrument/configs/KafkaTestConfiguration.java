package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.TkmPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentTokenFinder;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentDao;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

@TestConfiguration
public class KafkaTestConfiguration {
  @MockBean
  private EnrolledPaymentInstrumentDao dao;
  @MockBean
  private EnrolledPaymentInstrumentRepository repository;
  @MockBean
  private EnrolledPaymentInstrumentService paymentInstrumentService;
  @MockBean
  private TkmPaymentInstrumentService tkmPaymentInstrumentService;
  @MockBean
  private InstrumentTokenFinder instrumentTokenFinder;
  @Bean
  Validator validator() {
    try (final var factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }
}
