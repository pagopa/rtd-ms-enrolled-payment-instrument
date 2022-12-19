package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities;

import io.vavr.control.Either;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.AggregateRoot;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.PaymentInstrumentError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentTokenFinder;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * Aggregate domain object which describe an enrolled payment instrument.
 * <p>
 * It has a hashapan and a list of enabled vertical application.
 */
@Getter
@AllArgsConstructor
public class EnrolledPaymentInstrument extends AggregateRoot {

  public static EnrolledPaymentInstrument create(
          HashPan hashPan,
          SourceApp app
  ) {
    return create(hashPan, Set.of(app), "", "");
  }

  public static EnrolledPaymentInstrument create(
          HashPan hashPan,
          SourceApp app,
          String issuer,
          String network
  ) {
    return create(hashPan, Set.of(app), issuer, network);
  }

  public static EnrolledPaymentInstrument create(
          HashPan hashPan,
          Set<SourceApp> apps
  ) {
    return create(hashPan, apps, "", "");
  }

  public static EnrolledPaymentInstrument create(
          HashPan hashPan,
          Set<SourceApp> apps,
          String issuer,
          String network
  ) {
    if (apps.isEmpty()) throw new IllegalArgumentException("Apps cannot be empty");
    final var paymentInstrument = new EnrolledPaymentInstrument(
            null,
            hashPan,
            new HashSet<>(),
            null,
            PaymentInstrumentState.READY,
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
      if (!this.enabledApps.contains(sourceApp)) {
        this.registerEvent(new PaymentInstrumentEnrolled(hashPan, sourceApp));
      }
      this.enabledApps.add(sourceApp);
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
    this.par = par;
  }

  /**
   * Add a hashpan as a child this payment instrument (use for hash token)
   *
   * @param hashPan A valid hashpan, also an hash token is a valid hashpan
   */
  public void addHashPanChild(HashPan hashPan) {
    this.hashPanChildren.add(hashPan);
  }

  /**
   * Remove a child hashpan from payment instrument (use for hash token)
   *
   * @param hashPan A valid hashpan, also an hash token is a valid hashpan
   */
  public void removeHashPanChild(HashPan hashPan) {
    this.hashPanChildren.remove(hashPan);
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

  public Either<PaymentInstrumentError, Void> hydrateTokenAndParInfo(InstrumentTokenFinder tokenFinder) {
    final var requireHydrate = domainEvents().stream().anyMatch(PaymentInstrumentEnrolled.class::isInstance) && !enabledApps.isEmpty();
    if (requireHydrate) {
      return tokenFinder.findInstrumentInfo(hashPan)
              .andThen(this::hydrateTokenAndParInfo)
              .toEither()
              .mapLeft(it -> new PaymentInstrumentError("Failed to get token and par info", it))
              .map(it -> null);
    } else {
      return Either.right(null);
    }
  }

  private void hydrateTokenAndParInfo(InstrumentTokenInfo instrumentTokenInfo) {
    instrumentTokenInfo.getPar().ifPresent(this::associatePar);
    instrumentTokenInfo.getHashTokens().forEach(this::addHashPanChild);
  }
}
