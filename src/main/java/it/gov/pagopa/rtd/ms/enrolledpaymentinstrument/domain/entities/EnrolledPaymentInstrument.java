package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Aggregate domain object which describe an enrolled payment instrument.
 *
 * It has a hashapan and a list of enabled vertical application.
 */
@Data
@AllArgsConstructor
public class EnrolledPaymentInstrument {

  public static EnrolledPaymentInstrument create(
      HashPan hashPan,
      SourceApp sourceApp,
      String issuer,
      String network
  ) {
    return new EnrolledPaymentInstrument(
        null,
        hashPan,
        issuer,
        network,
        new HashSet<>(Collections.singletonList(sourceApp)),
        LocalDateTime.now(),
        null
    );
  }

  private final String id;
  private final HashPan hashPan;
  private String issuer;
  private String network;
  private Set<SourceApp> enabledApps;
  private LocalDateTime createAt;
  private LocalDateTime updatedAt;

  /**
   * Add source app as enabled from this instrument
   * @param sourceApp vertical domain application
   */
  public void enableApp(SourceApp sourceApp) {
    this.enabledApps.add(sourceApp);
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * Disable (aka remove) an enabled app from this instrument
   * @param sourceApp vertical domain application
   */
  public void disableApp(SourceApp sourceApp) {
    this.enabledApps.remove(sourceApp);
    this.updatedAt = LocalDateTime.now();
  }
}
