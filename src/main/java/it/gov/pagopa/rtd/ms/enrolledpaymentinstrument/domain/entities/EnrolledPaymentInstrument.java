package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.AggregateRoot;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.ChildTokenAssociated;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.ChildTokenDeleted;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.ParAssociated;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.PaymentInstrumentEnrolled;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Aggregate domain object which describe an enrolled payment instrument.
 * <p>
 * It has a hashapan and a list of enabled vertical application.
 */
@Getter
@AllArgsConstructor
public class EnrolledPaymentInstrument extends AggregateRoot {

  public static EnrolledPaymentInstrument createUnEnrolledInstrument(HashPan hashPan) {
    return createUnEnrolledInstrument(hashPan, "", "");
  }

  public static EnrolledPaymentInstrument createUnEnrolledInstrument(
          HashPan hashPan,
          String issuer,
          String network
  ) {
    return create(hashPan, new HashSet<>(), issuer, network);
  }

  public static EnrolledPaymentInstrument create(
          HashPan hashPan,
          Set<SourceApp> apps,
          String issuer,
          String network
  ) {
    final var paymentInstrument = new EnrolledPaymentInstrument(
            null,
            hashPan,
            new HashSet<>(),
            null,
            PaymentInstrumentState.NOT_ENROLLED,
            new HashSet<>(),
            issuer,
            network,
            0
    );
    apps.forEach(paymentInstrument::enableApp);
    return paymentInstrument;
  }

  private final String id;
  private final HashPan hashPan;
  private Set<HashPan> hashPanChildren;
  private String par;
  private PaymentInstrumentState state;
  private Set<SourceApp> enabledApps;
  private String issuer;
  private String network;
  private final int version;

  /**
   * Add source app as enabled from this instrument
   *
   * @param sourceApp vertical domain application
   */
  public void enableApp(SourceApp sourceApp) {
    this.state = state == PaymentInstrumentState.REVOKED ? this.state : PaymentInstrumentState.READY;
    if (this.state == PaymentInstrumentState.READY) {
      this.enabledApps.add(sourceApp);
      this.registerEvent(new PaymentInstrumentEnrolled(hashPan, sourceApp));
    }
  }

  /**
   * Disable (aka remove) an enabled app from this instrument
   *
   * @param sourceApp vertical domain application
   */
  public void disableApp(SourceApp sourceApp) {
    this.enabledApps.remove(sourceApp);
    this.state = this.enabledApps.isEmpty()
            ? PaymentInstrumentState.DELETE :
            this.state;
  }

  public void revokeInstrument() {
    this.state = PaymentInstrumentState.REVOKED;
  }

  /**
   * Allow to associate a par to this payment instrument
   *
   * @param par A valid par (not null and not blank)
   */
  public void associatePar(String par) {
    final boolean shouldUpdatePar = Optional.ofNullable(par)
            .map(it -> !it.equals(this.par)).orElse(false);
    if (shouldUpdatePar && state != PaymentInstrumentState.REVOKED) {
      this.par = par;
      registerEvent(new ParAssociated(hashPan, par, getEnabledApps()));
    }
  }

  public void addHashPanChildren(Set<HashPan> hashPans) {
    hashPans.forEach(this::addHashPanChild);
  }

  public void removeHashPanChildren(Set<HashPan> hashPans) {
    hashPans.forEach(this::removeHashPanChild);
  }

  /**
   * Add a hashpan as a child this payment instrument (use for hash token)
   *
   * @param hashPan A valid hashpan, also a hash token is a valid hashpan
   */
  public void addHashPanChild(HashPan hashPan) {
    if (!hashPanChildren.contains(hashPan) && state == PaymentInstrumentState.READY) {
      registerEvent(new ChildTokenAssociated(this.hashPan, hashPan, this.par, getEnabledApps()));
    }
    this.hashPanChildren.add(hashPan);
  }

  /**
   * Remove a child hashpan from payment instrument (use for hash token)
   *
   * @param hashPan A valid hashpan, also an hash token is a valid hashpan
   */
  public void removeHashPanChild(HashPan hashPan) {
    if(hashPanChildren.contains(hashPan) && state == PaymentInstrumentState.READY) {
      registerEvent(new ChildTokenDeleted(this.hashPan, hashPan, this.par, getEnabledApps()));
    }
    this.hashPanChildren.remove(hashPan);
  }

  public boolean isNotEnrolled() {
    return this.state == PaymentInstrumentState.NOT_ENROLLED;
  }

  public boolean isReady() {
    return this.state == PaymentInstrumentState.READY;
  }

  public boolean isRevoked() {
    return this.state == PaymentInstrumentState.REVOKED;
  }

  public boolean shouldBeDeleted() {
    return this.state == PaymentInstrumentState.DELETE;
  }
}
