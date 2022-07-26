package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.PaymentInstrumentEnrolled;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PaymentInstrumentTest {

  @Test
  void whenPaymentInstrumentIsCreateWithoutAppsThenMustBeNotEnrolled() {
    final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(TestUtils.generateRandomHashPan());
    assertTrue(paymentInstrument.isNotEnrolled());
    assertFalse(paymentInstrument.isReady());
  }

  @Test
  void whenPaymentInstrumentIsCreatedWithEnabledAppThenMustBeReady() {
    final var paymentInstrument = EnrolledPaymentInstrument.create(
            TestUtils.generateRandomHashPan(),
            Set.of(SourceApp.FA),
            "",
            ""
    );
    assertTrue(paymentInstrument.isReady());
    assertFalse(paymentInstrument.isNotEnrolled());
  }

  @Test
  void whenEnabledAppIsAddedThenPaymentInstrumentIsReady() {
    final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(TestUtils.generateRandomHashPan());
    paymentInstrument.enableApp(SourceApp.FA);
    assertTrue(paymentInstrument.isReady());
  }

  @Test
  void whenEnrollNewAppThenFirePaymentInstrumentEnrolledEvent() {
    final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(TestUtils.generateRandomHashPan());
    paymentInstrument.enableApp(SourceApp.ID_PAY);
    assertThat(paymentInstrument.domainEvents())
            .contains(new PaymentInstrumentEnrolled(paymentInstrument.getHashPan(), SourceApp.ID_PAY));
  }

  @Test
  void whenEnrollOverRevokedInstrumentThenNoFirePaymentInstrumentEnrolledEvent() {
    final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(TestUtils.generateRandomHashPan());
    paymentInstrument.revokeInstrument();
    paymentInstrument.enableApp(SourceApp.ID_PAY);
    assertThat(paymentInstrument.domainEvents()).isEmpty();
  }

  @Test
  void whenDisableAppAndKeepAtLeastOneThenPaymentInstrumentCantBeDeleted() {
    final var paymentInstrument = EnrolledPaymentInstrument.create(
            TestUtils.generateRandomHashPan(),
            Set.of(SourceApp.FA, SourceApp.ID_PAY),
            "",
            ""
    );
    paymentInstrument.disableApp(SourceApp.FA);
    assertFalse(paymentInstrument.shouldBeDeleted());
    assertTrue(paymentInstrument.isReady());
  }

  @Test
  void whenDisableAppAndKeepAtLeastOneThenPaymentInstrumentShouldBeDeleted() {
    final var paymentInstrument = EnrolledPaymentInstrument.create(
            TestUtils.generateRandomHashPan(),
            Set.of(SourceApp.FA, SourceApp.ID_PAY),
            "",
            ""
    );
    paymentInstrument.disableApp(SourceApp.FA);
    paymentInstrument.disableApp(SourceApp.ID_PAY);
    assertTrue(paymentInstrument.shouldBeDeleted());
    assertFalse(paymentInstrument.isReady());
  }

  @Test
  void whenRevokeAReadyInstrumentThenMustNotBeReady() {
    final var paymentInstrument = EnrolledPaymentInstrument.create(
            TestUtils.generateRandomHashPan(),
            Set.of(SourceApp.FA, SourceApp.ID_PAY),
            "",
            ""
    );
    paymentInstrument.revokeInstrument();
    assertTrue(paymentInstrument.isRevoked());
    assertFalse(paymentInstrument.isReady());
  }


  @Test
  void whenUnEnrollAllAppsFromRevokeCardThenShouldBeDeleted() {
    final var paymentInstrument = EnrolledPaymentInstrument.create(
            TestUtils.generateRandomHashPan(),
            Set.of(SourceApp.FA, SourceApp.ID_PAY),
            "",
            ""
    );
    paymentInstrument.revokeInstrument();
    paymentInstrument.disableApp(SourceApp.ID_PAY);
    paymentInstrument.disableApp(SourceApp.FA);
    assertFalse(paymentInstrument.isRevoked());
    assertTrue(paymentInstrument.shouldBeDeleted());
  }

  @Test
  void whenEnrollAppToRevokedInstrumentThenItKeepRevokedState() {
    final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(
            TestUtils.generateRandomHashPan(), "", ""
    );
    paymentInstrument.revokeInstrument();
    paymentInstrument.enableApp(SourceApp.ID_PAY);
    assertTrue(paymentInstrument.isRevoked());
    assertFalse(paymentInstrument.isReady());
  }

  @ParameterizedTest
  @ArgumentsSource(RandomPaymentInstrumentProvider.class)
  void whenUpdateParInfoThenInstrumentDoesntChangeState(EnrolledPaymentInstrument paymentInstrument) {
    final var currentState = paymentInstrument.getState();
    paymentInstrument.associatePar("par");
    assertEquals(currentState, paymentInstrument.getState());
  }

  @ParameterizedTest
  @ArgumentsSource(RandomPaymentInstrumentProvider.class)
  void whenUpdateChildHashPanThenInstrumentDoesntChangeState(EnrolledPaymentInstrument paymentInstrument) {
    final var currentState = paymentInstrument.getState();
    paymentInstrument.addHashPanChild(TestUtils.generateRandomHashPan());
    assertEquals(currentState, paymentInstrument.getState());
  }

  @ParameterizedTest
  @ArgumentsSource(RandomPaymentInstrumentProvider.class)
  void whenClearDomainEventsThenNoEventsAreAvailable(EnrolledPaymentInstrument paymentInstrument) {
    paymentInstrument.clearDomainEvents();
    assertThat(paymentInstrument.domainEvents()).isEmpty();
  }

  static class RandomPaymentInstrumentProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
      final var revokedCard = EnrolledPaymentInstrument.create(TestUtils.generateRandomHashPan(), Set.of(SourceApp.FA), "", "");
      revokedCard.revokeInstrument();
      return Stream.of(
              Arguments.of(EnrolledPaymentInstrument.createUnEnrolledInstrument(TestUtils.generateRandomHashPan(), "", "")),
              Arguments.of(EnrolledPaymentInstrument.create(TestUtils.generateRandomHashPan(), Set.of(SourceApp.FA), "", "")),
              Arguments.of(revokedCard)
      );
    }
  }
}
