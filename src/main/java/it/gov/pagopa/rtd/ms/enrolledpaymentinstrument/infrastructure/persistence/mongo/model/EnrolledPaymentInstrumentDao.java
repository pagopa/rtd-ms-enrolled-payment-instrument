package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface EnrolledPaymentInstrumentDao extends MongoRepository<EnrolledPaymentInstrumentEntity, String> { }
