package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.EnrolledPaymentInstrumentEntity;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.mapper.EnrolledPaymentInstrumentMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Optional;
import java.util.function.Supplier;

@AllArgsConstructor
@Slf4j
public class EnrolledPaymentInstrumentRepositoryImpl implements
        EnrolledPaymentInstrumentRepository {

    private final EnrolledPaymentInstrumentMapper mapper;
    private final EnrolledPaymentInstrumentDao dao;
    private final MongoTemplate mongoTemplate;

    public EnrolledPaymentInstrumentRepositoryImpl(EnrolledPaymentInstrumentDao dao, MongoTemplate template) {
        this.dao = dao;
        this.mongoTemplate = template;
        this.mapper = new EnrolledPaymentInstrumentMapper();
    }

    @Override
    public void save(EnrolledPaymentInstrument enrolledPaymentInstrument) {
        // mapping should be handled by a specific domain-to-entity mapper
        // find and replace ensure to update the same document based on hashPan property
        final var entity = mapper.toEntity(enrolledPaymentInstrument);
        final var query = Query.query(Criteria.where("hashPan").is(entity.getHashPan()));
        tryAndLog(() -> mongoTemplate.findAndReplace(
                query,
                entity,
                FindAndReplaceOptions.options().upsert().returnNew()
        ));
    }

    @Override
    public void delete(EnrolledPaymentInstrument enrolledPaymentInstrument) {
        final var query = Query.query(Criteria.where("hashPan").is(enrolledPaymentInstrument.getHashPan().getValue()));
        tryAndLog(() -> mongoTemplate.findAndRemove(query, EnrolledPaymentInstrumentEntity.class));
    }

    @Override
    public Optional<EnrolledPaymentInstrument> findByHashPan(String hashPan) {
        return dao.findByHashPan(hashPan).map(mapper::toDomain);
    }

    /**
     * Execute a function and try to catch an exception to log it in a safe way. Also, return
     * the computational result as optional, which will be not empty when the function complete successfully,
     * and empty when not.
     *
     * @param function function which can generate a and exception
     * @param <T>      Type of object returned by the function
     * @return The object returned by the function
     */
    private <T> Optional<T> tryAndLog(Supplier<T> function) {
        try {
            final var id = function.get();
            return Optional.ofNullable(id);
        } catch (Exception exception) {
            log.error("Error during write on mongodb {}", exception.getMessage());
        }
        return Optional.empty();
    }
}
