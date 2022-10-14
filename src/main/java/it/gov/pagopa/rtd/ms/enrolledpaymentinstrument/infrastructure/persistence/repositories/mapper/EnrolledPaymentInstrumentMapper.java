package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.mapper;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.PaymentInstrumentState;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.EnrolledPaymentInstrumentEntity;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

public class EnrolledPaymentInstrumentMapper {

  private static final String UPSERT_USER = "enrolled_payment_instrument";

  public EnrolledPaymentInstrument toDomain(EnrolledPaymentInstrumentEntity entity) {
    final var apps = entity.getApps().stream().map(SourceApp::valueOf).collect(Collectors.toSet());
    final var hashPanChildren = entity.getHashPanChildren().stream().map(HashPan::create).collect(Collectors.toSet());
    return new EnrolledPaymentInstrument(
            entity.getId(),
            HashPan.create(entity.getHashPan()),
            hashPanChildren,
            entity.getPar(),
            PaymentInstrumentState.valueOf(entity.getState()),
            apps,
            entity.getIssuer(),
            entity.getNetwork(),
            entity.getVersion()
    );
  }

  public EnrolledPaymentInstrumentEntity toEntity(EnrolledPaymentInstrument domain) {
    return EnrolledPaymentInstrumentEntity.builder()
            .id(domain.getId())
            .hashPan(domain.getHashPan().getValue())
            .network(domain.getNetwork())
            .issuer(domain.getIssuer())
            .par(domain.getPar())
            .hashPanChildren(domain.getHashPanChildren().stream().map(HashPan::getValue).collect(Collectors.toList()))
            .state(domain.getState().name())
            .apps(domain.getEnabledApps().stream().map(Enum::name).collect(Collectors.toList()))
            .insertAt(domain.getId() != null ? null : LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .insertUser(UPSERT_USER)
            .updateUser(UPSERT_USER)
            .version(domain.getVersion())
            .build();
  }
}
