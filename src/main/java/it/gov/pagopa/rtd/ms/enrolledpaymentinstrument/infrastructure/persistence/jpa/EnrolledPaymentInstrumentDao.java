package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.jpa;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.jpa.model.EnrolledPaymentInstrumentEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrolledPaymentInstrumentDao extends
    MongoRepository<EnrolledPaymentInstrumentEntity, String> { }
