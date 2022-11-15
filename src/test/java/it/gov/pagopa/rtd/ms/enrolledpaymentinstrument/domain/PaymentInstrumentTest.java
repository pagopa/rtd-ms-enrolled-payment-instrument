package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.*;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.ChildTokenAssociated;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.ChildTokenDeleted;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.ParAssociated;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.events.PaymentInstrumentEnrolled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

  @Test
  void whenUpdateParInfoThenInstrumentFiresParAssociatedEvent() {
    final var paymentInstrument = EnrolledPaymentInstrument.create(
            TestUtils.generateRandomHashPan(),
            Set.of(SourceApp.values()),
            "",
            ""
    );
    paymentInstrument.associatePar("par");
    assertThat(paymentInstrument.domainEvents()).contains(new ParAssociated(paymentInstrument.getHashPan(), "par", Set.of(SourceApp.values())));
  }

  @ParameterizedTest
  @ArgumentsSource(RandomPaymentInstrumentProvider.class)
  void whenUpdateParWithSameParThenNoParAssociatedEventIsFired(EnrolledPaymentInstrument paymentInstrument) {
    paymentInstrument.associatePar("par");
    paymentInstrument.clearDomainEvents();
    assertThat(paymentInstrument.domainEvents()).isEmpty();

    paymentInstrument.associatePar("par");
    assertThat(paymentInstrument.domainEvents()).isEmpty();
  }

  @ParameterizedTest
  @ArgumentsSource(RandomPaymentInstrumentProvider.class)
  void whenUpdateParInfoThenInstrumentDoesntChangeState(EnrolledPaymentInstrument paymentInstrument) {
    final var currentState = paymentInstrument.getState();
    paymentInstrument.associatePar("par");
    assertEquals(currentState, paymentInstrument.getState());
  }

  @Test
  void whenUpdateParToRevokeCardThenInstrumentDoesntUpdateItNeitherFireEvent() {
    final var paymentInstrument = EnrolledPaymentInstrument.create(TestUtils.generateRandomHashPan(), Set.of(SourceApp.values()), "", "");
    paymentInstrument.clearDomainEvents();
    paymentInstrument.associatePar("par");
    paymentInstrument.revokeInstrument();

    paymentInstrument.associatePar("par2");

    assertThat(paymentInstrument.getPar()).isEqualTo("par");
    assertThat(paymentInstrument.domainEvents()).containsOnly(new ParAssociated(paymentInstrument.getHashPan(), "par", Set.of(SourceApp.values())));
  }

  @ParameterizedTest
  @ArgumentsSource(RandomPaymentInstrumentProvider.class)
  void whenUpdateChildHashPanThenInstrumentDoesntChangeState(EnrolledPaymentInstrument paymentInstrument) {
    final var currentState = paymentInstrument.getState();
    paymentInstrument.addHashPanChild(TestUtils.generateRandomHashPan());
    assertEquals(currentState, paymentInstrument.getState());
  }

  @Test
  void whenAddChildHashPanThenTokenAssociatedEventIsFired() {
    final var rootHashPan = TestUtils.generateRandomHashPan();
    final var childHashPans = IntStream.of(2).mapToObj(i -> TestUtils.generateRandomHashPan()).collect(Collectors.toSet());
    final var paymentInstrument = EnrolledPaymentInstrument.create(rootHashPan, Set.of(SourceApp.ID_PAY), "", "");
    paymentInstrument.addHashPanChildren(childHashPans);

    assertThat(paymentInstrument.domainEvents()).containsAll(
            childHashPans.stream().map(child -> new ChildTokenAssociated(rootHashPan, child, null, Set.of(SourceApp.ID_PAY))).collect(Collectors.toSet())
    );
  }

  @Test
  void whenAddChildHashPanToUnEnrolledInstrumentThenNoTokenAssociatedEventIsFired() {
    final var rootHashPan = TestUtils.generateRandomHashPan();
    final var childHashPans = IntStream.of(2).mapToObj(i -> TestUtils.generateRandomHashPan()).collect(Collectors.toSet());
    final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(rootHashPan, "", "");
    paymentInstrument.addHashPanChildren(childHashPans);

    assertThat(paymentInstrument.domainEvents()).isEmpty();
  }

  @Test
  void whenAddExistingChildHashPanThenNoTokenAssociatedEventIsFired() {
    final var rootHashPan = TestUtils.generateRandomHashPan();
    final var childHashPans = IntStream.of(2).mapToObj(i -> TestUtils.generateRandomHashPan()).collect(Collectors.toSet());
    final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(rootHashPan, "", "");
    paymentInstrument.addHashPanChildren(childHashPans);
    paymentInstrument.clearDomainEvents();

    paymentInstrument.addHashPanChildren(childHashPans);
    assertThat(paymentInstrument.domainEvents()).isEmpty();
  }

  @Test
  void whenRemoveExistingChildHashPanThenTokenDeletedIsFired() {
    final var rootHashPan = TestUtils.generateRandomHashPan();
    final var childHashPans = IntStream.of(2).mapToObj(i -> TestUtils.generateRandomHashPan()).collect(Collectors.toSet());
    final var paymentInstrument = EnrolledPaymentInstrument.create(rootHashPan, Set.of(SourceApp.ID_PAY), "", "");
    paymentInstrument.addHashPanChildren(childHashPans);
    paymentInstrument.clearDomainEvents();

    paymentInstrument.removeHashPanChildren(childHashPans);
    assertThat(paymentInstrument.domainEvents()).containsAll(
            childHashPans.stream().map(child -> new ChildTokenDeleted(rootHashPan, child, null, Set.of(SourceApp.ID_PAY))).collect(Collectors.toSet())
    );
  }

  @Test
  void whenRemoveNonExistingChildHashPanThenNoTokenDeletedIsFired() {
    final var rootHashPan = TestUtils.generateRandomHashPan();
    final var childHashPans = IntStream.of(2).mapToObj(i -> TestUtils.generateRandomHashPan()).collect(Collectors.toSet());
    final var paymentInstrument = EnrolledPaymentInstrument.create(rootHashPan, Set.of(SourceApp.ID_PAY), "", "");
    paymentInstrument.clearDomainEvents();

    paymentInstrument.removeHashPanChildren(childHashPans);
    assertThat(paymentInstrument.domainEvents()).isEmpty();
  }

  @Test
  void whenRemoveExistingChildHashPanOnUnEnrolledInstrumentThenNoTokenDeletedIsFired() {
    final var rootHashPan = TestUtils.generateRandomHashPan();
    final var childHashPans = IntStream.of(2).mapToObj(i -> TestUtils.generateRandomHashPan()).collect(Collectors.toSet());
    final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(rootHashPan);

    paymentInstrument.removeHashPanChildren(childHashPans);
    assertThat(paymentInstrument.domainEvents()).isEmpty();
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
