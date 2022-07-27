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
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor
public class EnrolledPaymentInstrumentRepositoryImpl implements EnrolledPaymentInstrumentRepository {

  private final DomainMapper mapper;
  private final EnrolledPaymentInstrumentDao dao;
  private final MongoTemplate mongoTemplate;

  public EnrolledPaymentInstrumentRepositoryImpl(EnrolledPaymentInstrumentDao dao, MongoTemplate template) {
    this.dao = dao;
    this.mongoTemplate = template;
    this.mapper = new DomainMapper();
  }

  @Override
  public Future<String> save(EnrolledPaymentInstrument enrolledPaymentInstrument) {
    // mapping should be handled by a specific domain-to-entity mapper
    // find and replace ensure to update the same document based on hashPan property
    final var entity = mapper.toEntity(enrolledPaymentInstrument);
    final var query = Query.query(Criteria.where("hashPan").is(entity.getHashPan()));
    final var savedEntity = mongoTemplate.findAndReplace(
        query,
        entity,
        FindAndReplaceOptions.options().upsert().returnNew()
    );
    return savedEntity == null ?
        CompletableFuture.failedFuture(new Exception("Failed to save entity")) :
        CompletableFuture.completedFuture(savedEntity.getHashPan());
  }

  @Override
  public Optional<EnrolledPaymentInstrument> findByHashPan(String hashPan) {
    final var savedEntity = dao.findByHashPan(hashPan);
    return savedEntity.map(mapper::toDomain);
  }

  private static class DomainMapper {

    private static final String upsertUser = "enrolled_payment_instrument";

    EnrolledPaymentInstrument toDomain(EnrolledPaymentInstrumentEntity entity) {
      return new EnrolledPaymentInstrument(
          entity.getId(),
          HashPan.create(entity.getHashPan()),
          entity.getIssuer(),
          entity.getNetwork(),
          entity.getApps().stream().map(SourceApp::valueOf).collect(Collectors.toSet()),
          entity.getInsertAt(),
          entity.getUpdatedAt(),
          entity.get_etag()
      );
    }

    EnrolledPaymentInstrumentEntity toEntity(EnrolledPaymentInstrument domain) {
      return EnrolledPaymentInstrumentEntity.builder()
          .id(domain.getId())
          .hashPan(domain.getHashPan().getValue())
          .network(domain.getNetwork())
          .issuer(domain.getIssuer())
          .apps(domain.getEnabledApps().stream().map(Enum::name).collect(Collectors.toList()))
          .insertAt(domain.getCreateAt())
          .updatedAt(domain.getUpdatedAt())
          .insertUser(upsertUser)
          .updateUser(upsertUser)
          ._etag(domain.getVersion())
          .build();
    }
  }
}
