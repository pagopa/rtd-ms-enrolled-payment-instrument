package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories;

import com.mongodb.MongoCommandException;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.exception.WriteConflict;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model.EnrolledPaymentInstrumentDao;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model.EnrolledPaymentInstrumentEntity;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.mapper.EnrolledPaymentInstrumentMapper;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor
public class EnrolledPaymentInstrumentRepositoryImpl implements EnrolledPaymentInstrumentRepository {

  private static final int CONFLICT_WRITE_CODE = 112;

  private final EnrolledPaymentInstrumentMapper mapper;
  private final EnrolledPaymentInstrumentDao dao;
  private final MongoTemplate mongoTemplate;

  public EnrolledPaymentInstrumentRepositoryImpl(EnrolledPaymentInstrumentDao dao, MongoTemplate template) {
    this.dao = dao;
    this.mongoTemplate = template;
    this.mapper = new EnrolledPaymentInstrumentMapper();
  }

  @Override
  public Future<String> save(EnrolledPaymentInstrument enrolledPaymentInstrument) {
    // mapping should be handled by a specific domain-to-entity mapper
    // find and replace ensure to update the same document based on hashPan property
    final var savedId = catchWriteConflict(() -> {
      final var entity = mapper.toEntity(enrolledPaymentInstrument);
      final var query = Query.query(Criteria.where("hashPan").is(entity.getHashPan()));
      return mongoTemplate.findAndReplace(
          query,
          entity,
          FindAndReplaceOptions.options().upsert().returnNew()
      );
    });

    return savedId.map(it -> CompletableFuture.completedFuture(it.getHashPan()))
        .orElse(CompletableFuture.failedFuture(new Exception("Failed to save entity")));
  }

  @Override
  public boolean delete(EnrolledPaymentInstrument enrolledPaymentInstrument) {
    return catchWriteConflict(() -> mongoTemplate.findAndRemove(
        Query.query(Criteria.where("hashPan").is(enrolledPaymentInstrument.getHashPan().getValue())),
        EnrolledPaymentInstrumentEntity.class
    )).isPresent();
  }

  @Override
  public Optional<EnrolledPaymentInstrument> findByHashPan(String hashPan) {
    final var savedEntity = dao.findByHashPan(hashPan);
    return savedEntity.map(mapper::toDomain);
  }

  /**
   * Execute a function and try to catch a mongodb write conflict exceptions which is
   * mapped into a WriteConflict exception.
   * @param function function which can generate a mongodb conflict
   * @return The object returned by the function
   * @param <T> Type of object returned by the function
   * @throws WriteConflict The concurrency write conflict exception
   */
  private <T> Optional<T> catchWriteConflict(Supplier<T> function) throws WriteConflict {
    try {
      final var id = function.get();
      return Optional.ofNullable(id);
    } catch (UncategorizedMongoDbException uncategorizedMongoDbException) {
      if (uncategorizedMongoDbException.getCause() instanceof MongoCommandException) {
        if (((MongoCommandException) uncategorizedMongoDbException.getCause()).getErrorCode() == CONFLICT_WRITE_CODE) {
          throw new WriteConflict(uncategorizedMongoDbException);
        }
      }
    }
    return Optional.empty();
  }


}
