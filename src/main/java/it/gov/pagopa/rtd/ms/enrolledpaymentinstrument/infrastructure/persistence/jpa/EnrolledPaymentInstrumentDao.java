package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.jpa;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.jpa.model.EnrolledPaymentInstrumentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrolledPaymentInstrumentDao extends JpaRepository<EnrolledPaymentInstrumentEntity, UUID> { }
