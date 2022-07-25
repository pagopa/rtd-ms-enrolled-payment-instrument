package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class EnrolledPaymentInstrument {

  public static EnrolledPaymentInstrument create(
      HashPan hashPan,
      App app
  ) {
    return new EnrolledPaymentInstrument(
        null,
        hashPan,
        new HashSet<>(Collections.singletonList(app)),
        LocalDateTime.now(),
        null
    );
  }

  private final String id;
  private final HashPan hashPan;
  private Set<App> enabledApps;
  private LocalDateTime createAt;
  private LocalDateTime updatedAt;

  public void enrollFrom(App app) {
    this.enabledApps.add(app);
    this.updatedAt = LocalDateTime.now();
  }


}
