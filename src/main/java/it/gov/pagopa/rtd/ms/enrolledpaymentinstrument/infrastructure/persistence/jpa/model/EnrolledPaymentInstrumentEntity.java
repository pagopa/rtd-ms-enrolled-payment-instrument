package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.jpa.model;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.ToString;

@Entity
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Setter(value = AccessLevel.PACKAGE)
@Data
@Table(name = "rtd_enrolled_payment_instrument")
public class EnrolledPaymentInstrumentEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id")
  private UUID id;

  @Column(name = "something")
  private String something;

  public EnrolledPaymentInstrumentEntity() {
  }
}
