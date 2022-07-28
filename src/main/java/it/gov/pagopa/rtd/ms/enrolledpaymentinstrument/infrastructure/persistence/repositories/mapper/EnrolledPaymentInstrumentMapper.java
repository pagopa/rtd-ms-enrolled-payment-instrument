package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.mapper;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model.EnrolledPaymentInstrumentEntity;
import java.util.stream.Collectors;

public class EnrolledPaymentInstrumentMapper {

  private static final String upsertUser = "enrolled_payment_instrument";

  public EnrolledPaymentInstrument toDomain(EnrolledPaymentInstrumentEntity entity) {
    final var apps = entity.getApps().stream().map(SourceApp::valueOf).collect(Collectors.toSet());
    return new EnrolledPaymentInstrument(
        entity.getId(),
        HashPan.create(entity.getHashPan()),
        entity.getIssuer(),
        entity.getNetwork(),
        entity.getApps().stream().map(SourceApp::valueOf).collect(Collectors.toSet()),
        entity.getInsertAt(),
        entity.getUpdatedAt(),
        apps.isEmpty()
    );
  }

  public EnrolledPaymentInstrumentEntity toEntity(EnrolledPaymentInstrument domain) {
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
        .build();
  }
}
