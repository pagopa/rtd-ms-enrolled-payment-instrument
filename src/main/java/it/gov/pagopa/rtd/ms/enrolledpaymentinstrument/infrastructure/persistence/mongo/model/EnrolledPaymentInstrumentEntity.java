package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("enrolled_payment_instrument")
@Data
@Setter(value = AccessLevel.PACKAGE)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class EnrolledPaymentInstrumentEntity {

  @Id
  private String id;

  private String something;

  public EnrolledPaymentInstrumentEntity() {
  }
}
