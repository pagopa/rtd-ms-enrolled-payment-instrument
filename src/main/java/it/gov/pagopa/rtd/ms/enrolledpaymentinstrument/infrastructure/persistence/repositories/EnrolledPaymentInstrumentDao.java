package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model.EnrolledPaymentInstrumentEntity;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EnrolledPaymentInstrumentDao extends MongoRepository<EnrolledPaymentInstrumentEntity, String> {
  Optional<EnrolledPaymentInstrumentEntity> findByHashPan(String hashPan);
}