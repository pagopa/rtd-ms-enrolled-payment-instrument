package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model.EnrolledPaymentInstrumentDao;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model.EnrolledPaymentInstrumentEntity;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EnrolledPaymentInstrumentRepositoryImpl implements EnrolledPaymentInstrumentRepository {

  private final EnrolledPaymentInstrumentDao dao;

  @Override
  public Future<String> save(EnrolledPaymentInstrument enrolledPaymentInstrument) {
    // mapping should be handled by a specific domain-to-entity mapper
    final var entity = EnrolledPaymentInstrumentEntity.builder()
        .something(
            enrolledPaymentInstrument.getSomething1() + ";" + enrolledPaymentInstrument.getSomething2())
        .build();
    final var savedEntity = dao.save(entity);
    return CompletableFuture.completedFuture(savedEntity.getId());
  }

  @Override
  public Optional<EnrolledPaymentInstrument> findById(String id) {
    final var savedEntity = dao.findById(id);

    return savedEntity.map(it -> {
      final var somethings = it.getSomething().split(";");
      return new EnrolledPaymentInstrument(somethings[0], somethings[1]);
    });
  }
}
