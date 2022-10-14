package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.TkmPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentDao;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import javax.validation.Validation;
import javax.validation.Validator;

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
  @Bean
  Validator validator() {
    try (final var factory = Validation.buildDefaultValidatorFactory()) {
      return factory.getValidator();
    }
  }
}
