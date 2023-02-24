package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.util.List;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

@Document("enrolled_payment_instrument")
@Data
@Setter(value = AccessLevel.PACKAGE)
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor
public class EnrolledPaymentInstrumentEntity {

  @Id
  private String id;

  @Indexed(unique = true)
  @Field(name = "hashPan")
  private String hashPan;

  @Indexed
  @Field("hashPanChildren")
  private List<String> hashPanChildren;

  @Indexed
  @Field(name = "enabledApps")
  private List<String> apps;

  @Field(name = "par")
  private String par;

  @Field(name = "state")
  private String state;

  @Field(name = "issuer")
  private String issuer;

  @Field(name = "network")
  private String network;

  @Field(name = "insertAt")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime insertAt;

  @Field(name = "updatedAt")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime updatedAt;

  @Field(name = "insertUser")
  private String insertUser;

  @Field(name = "updateUser")
  private String updateUser;

  @Field(name = "exported")
  private boolean exported;

  @Version
  private int version;

  @Field(name = "hashPanExports")
  private List<HashPan> hashPanExports;
}
