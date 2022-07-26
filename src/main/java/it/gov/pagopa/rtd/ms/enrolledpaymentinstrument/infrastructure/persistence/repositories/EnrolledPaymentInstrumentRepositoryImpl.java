package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model.EnrolledPaymentInstrumentDao;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model.EnrolledPaymentInstrumentEntity;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EnrolledPaymentInstrumentRepositoryImpl implements EnrolledPaymentInstrumentRepository {

  private final DomainMapper mapper;
  private final EnrolledPaymentInstrumentDao dao;

  public EnrolledPaymentInstrumentRepositoryImpl(EnrolledPaymentInstrumentDao dao) {
    this.dao = dao;
    this.mapper = new DomainMapper();
  }

  @Override
  public Future<String> save(EnrolledPaymentInstrument enrolledPaymentInstrument) {
    // mapping should be handled by a specific domain-to-entity mapper
    final var entity = mapper.toEntity(enrolledPaymentInstrument);
    final var savedEntity = dao.save(entity);
    return CompletableFuture.completedFuture(savedEntity.getId());
  }

  @Override
  public Optional<EnrolledPaymentInstrument> findById(String id) {
    final var savedEntity = dao.findByHashPan(id);
    return savedEntity.map(mapper::toDomain);
  }

  private static class DomainMapper {

    private static String upsertUser = "enrolled_payment_instrument";

    EnrolledPaymentInstrument toDomain(EnrolledPaymentInstrumentEntity entity) {
      return new EnrolledPaymentInstrument(
          entity.getId(),
          HashPan.create(entity.getHashPan()),
          entity.getApps().stream().map(SourceApp::valueOf).collect(Collectors.toSet()),
          entity.getInsertAt(),
          entity.getUpdatedAt()
      );
    }

    EnrolledPaymentInstrumentEntity toEntity(EnrolledPaymentInstrument domain) {
      return EnrolledPaymentInstrumentEntity.builder()
          .id(domain.getId())
          .hashPan(domain.getHashPan().getValue())
          .apps(domain.getEnabledApps().stream().map(Enum::name).collect(Collectors.toList()))
          .insertAt(domain.getCreateAt())
          .updatedAt(domain.getUpdatedAt())
          .insertUser(upsertUser)
          .updateUser(upsertUser)
          .build();
    }
  }
}
