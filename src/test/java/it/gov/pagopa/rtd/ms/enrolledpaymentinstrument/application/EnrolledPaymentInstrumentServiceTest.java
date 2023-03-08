package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import io.vavr.control.Try;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand.Operation;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.ExportCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.EnrollAckError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.ExportError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.InstrumentTokenInfo;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollNotifyService;
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
import java.time.OffsetDateTime;
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
  private EnrollNotifyService enrollAckService;

  @Autowired
  private InstrumentTokenFinder instrumentTokenFinder;

  @Autowired
  private EnrolledPaymentInstrumentService service;

  @BeforeEach
  void setup() {
    doReturn(true).when(enrollAckService).confirmEnroll(any(), any(), any());
    doReturn(true).when(enrollAckService).confirmExport(any(), any());
    doReturn(Try.success(new InstrumentTokenInfo(TestUtils.generateRandomHashPan(), "", List.of())))
            .when(instrumentTokenFinder)
            .findInstrumentInfo(any());
  }

  @AfterEach
  void cleanUp() {
    Mockito.reset(repository, enrollAckService, instrumentTokenFinder);
  }

  @DisplayName("must enable payment instrument for a specific source app")
  @Test
  void whenCommandEnablePaymentInstrumentThenEnableForSpecifiedApp() {
    final var argument = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    final var command = new EnrollPaymentInstrumentCommand(
        TEST_HASH_PAN.getValue(),
        SourceApp.ID_PAY.name(),
        Operation.CREATE
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
        Operation.CREATE
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
            Operation.CREATE
    );
    service.handle(command);

    verify(enrollAckService).confirmEnroll(eq(SourceApp.FA), eq(TEST_HASH_PAN), any());
  }

  // idempotency of card enrollment
  @Test
  void whenCommandEnableExistingAppThenSendAck() {
    final var fullEnrolledInstrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, Set.of(SourceApp.values()), null, null);
    fullEnrolledInstrument.clearDomainEvents();
    when(repository.findByHashPan(any())).thenReturn(Optional.of(fullEnrolledInstrument));

    final var command = new EnrollPaymentInstrumentCommand(
            TEST_HASH_PAN.getValue(),
            SourceApp.FA.name(),
            Operation.CREATE
    );
    service.handle(command);
    verify(enrollAckService, times(1)).confirmEnroll(eq(SourceApp.FA), eq(TEST_HASH_PAN), any());
  }

  @Test
  void whenSendAckFailThenThrowsException() {
    final var command = new EnrollPaymentInstrumentCommand(
            TEST_HASH_PAN.getValue(),
            SourceApp.FA.name(),
            Operation.CREATE
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
        Operation.DELETE
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
            Operation.CREATE
    );
    when(instrumentTokenFinder.findInstrumentInfo(any())).thenReturn(Try.success(stubTokenInfo));

    service.handle(enrollCommand);

    verify(repository, times(1)).save(captor.capture());

    assertThat(captor.getValue())
            .matches(it -> it.getPar().equals("123"))
            .satisfies(it -> assertThat(it.getHashPanChildren()).hasSameElementsAs(hashTokens));
  }

  @Test
  void whenEnrollInstrumentAndTokenFinderFailsThenCompleteEnroll() {
    final var enrollCommand = new EnrollPaymentInstrumentCommand(
            TestUtils.generateRandomHashPanAsString(),
            SourceApp.ID_PAY.toString(),
            Operation.CREATE
    );
    when(instrumentTokenFinder.findInstrumentInfo(any())).thenReturn(Try.failure(new RuntimeException("Fail")));

    service.handle(enrollCommand);

    verify(repository, times(1)).save(any());
    verify(enrollAckService, times(1)).confirmEnroll(eq(SourceApp.ID_PAY), any(), any());
  }

  @Test
  void whenHandleExportCommandForExistingPaymentInstrumentThenIsMarkedAsExported() {
    final var captor = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    final var exportCommand = new ExportCommand(TestUtils.generateRandomHashPan().getValue(), OffsetDateTime.now());
    when(repository.findByHashPan(any())).thenReturn(Optional.of(EnrolledPaymentInstrument.create(HashPan.create(exportCommand.getHashPan()), SourceApp.ID_PAY)));
    service.handle(exportCommand);
    verify(repository, times(1)).save(captor.capture());

    assertThat(captor.getValue().isExported()).isTrue();
  }

  @Test
  void whenHandleExportCommandForExistingPaymentInstrumentThenSendPaymentInstrumentExported() {
    final var exportCommand = new ExportCommand(TestUtils.generateRandomHashPan().getValue(), OffsetDateTime.now());
    when(repository.findByHashPan(any())).thenReturn(Optional.of(EnrolledPaymentInstrument.create(HashPan.create(exportCommand.getHashPan()), SourceApp.ID_PAY)));
    service.handle(exportCommand);
    verify(enrollAckService, times(1)).confirmExport(any(), any());
  }

  @Test
  void whenHandleExportCommandForAnExportedPaymentInstrumentThenSendPaymentInstrumentExported() {
    final var exportCommand = new ExportCommand(TestUtils.generateRandomHashPan().getValue(), OffsetDateTime.now());
    final var paymentInstrument = EnrolledPaymentInstrument.create(HashPan.create(exportCommand.getHashPan()), SourceApp.ID_PAY);
    paymentInstrument.markAsExported();
    paymentInstrument.clearDomainEvents();
    when(repository.findByHashPan(any())).thenReturn(Optional.of(paymentInstrument));
    service.handle(exportCommand);
    verify(enrollAckService, times(1)).confirmExport(any(), any());
  }

  @Test
  void whenHandleExportCommandForNonExistingInstrumentThenIsNotUpdated() {
    final var exportCommand = new ExportCommand(TestUtils.generateRandomHashPan().getValue(), OffsetDateTime.now());
    service.handle(exportCommand);
    verify(repository, times(0)).save(any());
  }

  @Test
  void whenNotifyExportFailThenThrowsException() {
    final var command = new ExportCommand(
            TEST_HASH_PAN.getValue(),
            OffsetDateTime.now()
    );
    when(repository.findByHashPan(any())).thenReturn(Optional.of(EnrolledPaymentInstrument.create(HashPan.create(command.getHashPan()), SourceApp.ID_PAY)));
    doReturn(false).when(enrollAckService).confirmExport(any(), any());
    assertThrowsExactly(ExportError.class, () -> service.handle(command));
  }

  @TestConfiguration
  @Import({DomainEventPublisher.class, EnrolledPaymentInstrumentEventListener.class})
  static class Config {

    @MockBean
    private EnrolledPaymentInstrumentRepository repository;

    @MockBean
    private EnrollNotifyService enrollAckService;

    @MockBean
    private InstrumentTokenFinder instrumentTokenFinder;

    @Bean
    EnrolledPaymentInstrumentService service(@Autowired DomainEventPublisher eventPublisher) {
      return new EnrolledPaymentInstrumentService(repository, instrumentTokenFinder, eventPublisher);
    }
  }

}
