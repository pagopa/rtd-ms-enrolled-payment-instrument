package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EnrolledPaymentInstrumentDao extends MongoRepository<EnrolledPaymentInstrumentEntity, String> {
  Optional<EnrolledPaymentInstrumentEntity> findByHashPan(String hashPan);
}
