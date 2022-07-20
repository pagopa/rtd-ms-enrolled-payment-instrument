package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.jpa.EnrolledPaymentInstrumentDao;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.jpa.model.EnrolledPaymentInstrumentEntity;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class EnrolledPaymentInstrumentRepositoryImpl implements
    EnrolledPaymentInstrumentRepository {

  // an example
  private EnrolledPaymentInstrumentDao dao;

  @Override
  public Future<String> save(EnrolledPaymentInstrument enrolledPaymentInstrument) {
    // mapping should be handled by a specific domain-to-entity mapper
    final var entity = EnrolledPaymentInstrumentEntity.builder()
        .something(
            enrolledPaymentInstrument.getSomething1() + enrolledPaymentInstrument.getSomething2())
        .build();
    final var savedEntity = dao.save(entity);
    return CompletableFuture.completedFuture(savedEntity.getSomething());
  }

}
