package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentInstrumentTest {

  @Test
  void whenPaymentInstrumentIsCreateWithoutAppsThenMustBeNotEnrolled() {
    final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(
            TestUtils.generateRandomHashPan(),
            "",
            ""
    );

    assertTrue(paymentInstrument.isNotEnrolled());
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
  }

  @Test
  void whenEnabledAppIsAddedThenPaymentInstrumentIsReady() {
    final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(
            TestUtils.generateRandomHashPan(),
            "",
            ""
    );
    paymentInstrument.enableApp(SourceApp.FA);

    assertTrue(paymentInstrument.isReady());
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
  }
}
