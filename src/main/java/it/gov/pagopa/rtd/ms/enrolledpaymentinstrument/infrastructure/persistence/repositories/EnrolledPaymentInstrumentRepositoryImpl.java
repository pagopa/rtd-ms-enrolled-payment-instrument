package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.mapper.EnrolledPaymentInstrumentMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class EnrolledPaymentInstrumentRepositoryImpl implements
        EnrolledPaymentInstrumentRepository {

  private final EnrolledPaymentInstrumentMapper mapper;
  private final EnrolledPaymentInstrumentDao dao;

  public EnrolledPaymentInstrumentRepositoryImpl(EnrolledPaymentInstrumentDao dao) {
    this.dao = dao;
    this.mapper = new EnrolledPaymentInstrumentMapper();
  }

  @Override
  public void save(EnrolledPaymentInstrument enrolledPaymentInstrument) {
    // mapping should be handled by a specific domain-to-entity mapper
    // find and replace ensure to update the same document based on hashPan property
    dao.save(mapper.toEntity(enrolledPaymentInstrument));
  }

  @Override
  public void delete(EnrolledPaymentInstrument enrolledPaymentInstrument) {
    dao.deleteByHashPan(enrolledPaymentInstrument.getHashPan().getValue());
  }

  @Override
  public Optional<EnrolledPaymentInstrument> findByHashPan(String hashPan) {
    return dao.findByHashPan(hashPan).map(mapper::toDomain);
  }
}
