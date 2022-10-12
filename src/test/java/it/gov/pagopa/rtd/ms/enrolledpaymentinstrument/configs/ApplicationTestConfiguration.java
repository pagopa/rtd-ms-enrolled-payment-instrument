package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentDao;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationTestConfiguration {
  @MockBean
  private EnrolledPaymentInstrumentDao dao;
  @MockBean
  private EnrolledPaymentInstrumentRepository repository;
  @MockBean
  private InstrumentRevokeNotificationService revokeService;
}
