package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.EnrolledPaymentInstrumentEntity;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.mapper.EnrolledPaymentInstrumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Optional;

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
    mongoTemplate.save(mapper.toEntity(enrolledPaymentInstrument));
  }

  @Override
  public void delete(EnrolledPaymentInstrument enrolledPaymentInstrument) {
    final var query = Query.query(Criteria.where("hashPan").is(enrolledPaymentInstrument.getHashPan().getValue()));
    mongoTemplate.findAndRemove(query, EnrolledPaymentInstrumentEntity.class);
  }

  @Override
  public Optional<EnrolledPaymentInstrument> findByHashPan(String hashPan) {
    return dao.findByHashPan(hashPan).map(mapper::toDomain);
  }
}
