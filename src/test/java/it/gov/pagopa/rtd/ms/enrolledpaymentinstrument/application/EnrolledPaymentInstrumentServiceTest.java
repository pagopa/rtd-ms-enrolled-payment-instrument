package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import io.vavr.control.Try;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.EnrollAckError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.PaymentInstrumentError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.InstrumentTokenInfo;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollAckService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentTokenFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.ConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@Import({EnrolledPaymentInstrumentServiceTest.Config.class, ValidationAutoConfiguration.class})
class EnrolledPaymentInstrumentServiceTest {

  private static final HashPan TEST_HASH_PAN = TestUtils.generateRandomHashPan();

  @Autowired
  private EnrolledPaymentInstrumentRepository repository;

  @Autowired
  private EnrollAckService enrollAckService;

  @Autowired
  private InstrumentTokenFinder instrumentTokenFinder;

  @Autowired
  private EnrolledPaymentInstrumentService service;

  @BeforeEach
  void setup() {
    doReturn(true).when(enrollAckService).confirmEnroll(any(), any(), any());
    doReturn(Try.success(new InstrumentTokenInfo(TestUtils.generateRandomHashPan(), "", List.of())))
            .when(instrumentTokenFinder)
            .findInstrumentInfo(any());
  }

  @AfterEach
  void cleanUp() {
    Mockito.reset(repository, enrollAckService);
  }

  @DisplayName("must enable payment instrument for a specific source app")
  @Test
  void whenCommandEnablePaymentInstrumentThenEnableForSpecifiedApp() {
    final var argument = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    final var command = new EnrollPaymentInstrumentCommand(
        TEST_HASH_PAN.getValue(),
        SourceApp.ID_PAY.name(),
        Operation.CREATE,
        null,
        null
    );

    service.handle(command);

    Mockito.verify(repository).save(argument.capture());
    assertEquals(TEST_HASH_PAN, argument.getValue().getHashPan());
    assertEquals(Collections.singleton(SourceApp.ID_PAY), argument.getValue().getEnabledApps());
  }

  @DisplayName("must be idempotent when enroll for same app")
  @Test
  void mustBeIdempotentWhenEnableSameApp() {
    final var argument = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    final var commands = IntStream.range(0, 3).mapToObj(i -> new EnrollPaymentInstrumentCommand(
        TEST_HASH_PAN.getValue(),
        SourceApp.ID_PAY.name(),
        Operation.CREATE,
        null,
        null
    )).collect(Collectors.toList());

    commands.forEach(command -> service.handle(command));
    Mockito.verify(repository, Mockito.times(3)).save(argument.capture());

    assertEquals(TEST_HASH_PAN, argument.getValue().getHashPan());
    assertEquals(Collections.singleton(SourceApp.ID_PAY), argument.getValue().getEnabledApps());
  }

  @Test
  void whenCommandEnableAppThenSendAck() {
    final var command = new EnrollPaymentInstrumentCommand(
            TEST_HASH_PAN.getValue(),
            SourceApp.FA.name(),
            Operation.CREATE,
            null,
            null
    );
    service.handle(command);

    verify(enrollAckService).confirmEnroll(eq(SourceApp.FA), eq(TEST_HASH_PAN), any());
  }

  @Test
  void whenCommandEnableExistingAppThenSendAck() {
    final var fullEnrolledInstrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, Set.of(SourceApp.values()), null, null);
    fullEnrolledInstrument.clearDomainEvents();
    when(repository.findByHashPan(any())).thenReturn(Optional.of(fullEnrolledInstrument));

    final var command = new EnrollPaymentInstrumentCommand(
            TEST_HASH_PAN.getValue(),
            SourceApp.FA.name(),
            Operation.CREATE,
            null,
            null
    );
    service.handle(command);
    verify(enrollAckService).confirmEnroll(eq(SourceApp.FA), eq(TEST_HASH_PAN), any());
  }

  @Test
  void whenSendAckFailThenThrowsException() {
    final var command = new EnrollPaymentInstrumentCommand(
            TEST_HASH_PAN.getValue(),
            SourceApp.FA.name(),
            Operation.CREATE,
            null,
            null
    );
    doReturn(false).when(enrollAckService).confirmEnroll(any(), any(), any());
    assertThrowsExactly(EnrollAckError.class, () -> service.handle(command));
  }

  @DisplayName("must disable payment instrument for a specific source app")
  @Test
  void mustDisablePaymentInstrumentForSpecificApp() {
    final var fullEnrolledInstrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, new HashSet<>(Arrays.asList(SourceApp.values())), null, null);
    final var argument = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    final var command = new EnrollPaymentInstrumentCommand(
        TEST_HASH_PAN.getValue(),
        SourceApp.FA.name(),
        Operation.DELETE,
        null,
        null
    );

    when(repository.findByHashPan(any())).thenReturn(Optional.of(fullEnrolledInstrument));

    service.handle(command);
    Mockito.verify(repository).save(argument.capture());

    assertEquals(TEST_HASH_PAN, argument.getValue().getHashPan());
    assertTrue(argument.getValue().getEnabledApps().size() > 0);
    assertTrue(argument.getValue().getEnabledApps().stream().noneMatch(i -> i == SourceApp.FA));
  }

  @DisplayName("must delete the payment instrument when enabled apps are empties")
  @Test
  void mustDeleteWhenAppsAreEmpties() {
    final var fullEnrolledInstrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, new HashSet<>(Arrays.asList(SourceApp.values())), null, null);
    final var commands = Arrays.stream(SourceApp.values()).map(app -> new EnrollPaymentInstrumentCommand(
        TEST_HASH_PAN.getValue(),
        app.name(),
        Operation.DELETE,
        null,
        null
    ));

    when(repository.findByHashPan(any())).thenReturn(Optional.of(fullEnrolledInstrument));

    commands.forEach(command -> service.handle(command));

    Mockito.verify(repository, Mockito.times(1)).delete(any());
  }

  @DisplayName("must throw exception when command is invalid")
  @Test
  void mustThrowExceptionWhenCommandIsInvalid() {
    final var invalidCommands = Arrays.asList(
        new EnrollPaymentInstrumentCommand(TEST_HASH_PAN.getValue(), "", Operation.CREATE, null, null),
        new EnrollPaymentInstrumentCommand(TEST_HASH_PAN.getValue(), null, Operation.CREATE, null, null),
        new EnrollPaymentInstrumentCommand("", SourceApp.ID_PAY.name(), Operation.CREATE, null, null),
        new EnrollPaymentInstrumentCommand(null, SourceApp.ID_PAY.name(), Operation.CREATE, null, null),
        new EnrollPaymentInstrumentCommand("", "", Operation.CREATE, null, null),
        new EnrollPaymentInstrumentCommand(null, null, Operation.CREATE, null, null)
    );

    assertTrue(invalidCommands.stream().noneMatch(command -> {
      try {
        service.handle(command);
        return true;
      } catch (ConstraintViolationException | IllegalArgumentException t) {
        return false;
      }
    }));
  }

  @Test
  void mustRetrieveTokenParInfoWhenEnroll() {
    final var captor = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    final var hashPan = TestUtils.generateRandomHashPan();
    final var hashTokens = IntStream.range(0, 4).mapToObj(i -> TestUtils.generateRandomHashPan()).collect(Collectors.toList());
    final var stubTokenInfo = new InstrumentTokenInfo(hashPan, "123", hashTokens);
    final var enrollCommand = new EnrollPaymentInstrumentCommand(
            TestUtils.generateRandomHashPanAsString(),
            SourceApp.ID_PAY.toString(),
            Operation.CREATE,
            "",
            ""
    );
    when(instrumentTokenFinder.findInstrumentInfo(any())).thenReturn(Try.success(stubTokenInfo));

    service.handle(enrollCommand);

    verify(repository, times(1)).save(captor.capture());

    assertThat(captor.getValue())
            .matches(it -> it.getPar().equals("123"))
            .satisfies(it -> assertThat(it.getHashPanChildren()).hasSameElementsAs(hashTokens));
  }

  @Test
  void mustThrowAnExceptionWhenTokenParHydrateFails() {
    final var enrollCommand = new EnrollPaymentInstrumentCommand(
            TestUtils.generateRandomHashPanAsString(),
            SourceApp.ID_PAY.toString(),
            Operation.CREATE,
            "",
            ""
    );
    when(instrumentTokenFinder.findInstrumentInfo(any())).thenReturn(Try.failure(new RuntimeException("Fail")));

    assertThrows(PaymentInstrumentError.class, () -> service.handle(enrollCommand));
  }



  @TestConfiguration
  @Import({DomainEventPublisher.class, EnrolledPaymentInstrumentEventListener.class})
  static class Config {

    @MockBean
    private EnrolledPaymentInstrumentRepository repository;

    @MockBean
    private EnrollAckService enrollAckService;

    @MockBean
    private InstrumentTokenFinder instrumentTokenFinder;

    @Bean
    EnrolledPaymentInstrumentService service(@Autowired DomainEventPublisher eventPublisher) {
      return new EnrolledPaymentInstrumentService(repository, instrumentTokenFinder, eventPublisher);
    }
  }

}
