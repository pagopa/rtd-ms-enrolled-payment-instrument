package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.FailedToNotifyRevoke;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollNotifyService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import org.junit.jupiter.api.*;
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

import jakarta.validation.ConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@Import({TkmPaymentInstrumentServiceTest.Config.class, ValidationAutoConfiguration.class})
class TkmPaymentInstrumentServiceTest {

  @Autowired
  private EnrolledPaymentInstrumentRepository repository;

  @Autowired
  private InstrumentRevokeNotificationService revokeService;

  @Autowired
  private TkmPaymentInstrumentService service;

  private ArgumentCaptor<EnrolledPaymentInstrument> paymentInstrumentArgumentCaptor;

  @BeforeEach
  void setUp() {
    paymentInstrumentArgumentCaptor = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    doReturn(true).when(revokeService).notifyRevoke(any(), any(), any());
  }

  @AfterEach()
  void clean() {
    Mockito.reset(repository, revokeService);
  }


  @Nested
  @DisplayName("Tests for update command")
  class UpdateCommandCases {
    @Test
    void whenTkmUpdateANonExistingPaymentInstrumentThenNothingIsCreated() {
      final var updateCommand = new TkmUpdateCommand(
              TestUtils.generateRandomHashPan().getValue(),
              null,
              Collections.emptyList()
      );
      service.handle(updateCommand);
      Mockito.verify(repository, times(0)).save(any());
    }

    @Test
    void whenTkmUpdateWithNewTokensThenPaymentInstrumentShouldAddIt() {
      final var hashPan = TestUtils.generateRandomHashPan();
      final var tokenCommands = TestUtils.generateRandomUpdateTokenCommand(10);
      final var tokenToBeInsert = tokenCommands
              .stream()
              .filter(it -> it.getAction() == TkmUpdateCommand.TkmTokenCommand.Action.UPDATE)
              .map(it -> HashPan.create(it.getHashPan()))
              .collect(Collectors.toSet());
      final var updateCommand = new TkmUpdateCommand(
              hashPan.getValue(),
              null,
              tokenCommands
      );
      Mockito.when(repository.findByHashPan(hashPan.getValue())).thenReturn(
              Optional.of(EnrolledPaymentInstrument.create(hashPan, Set.of(SourceApp.ID_PAY), "", ""))
      );
      service.handle(updateCommand);
      Mockito.verify(repository).save(paymentInstrumentArgumentCaptor.capture());

      final var savedInstrument = paymentInstrumentArgumentCaptor.getValue();
      assertTrue(savedInstrument.isReady());
      assertThat(tokenToBeInsert).hasSameElementsAs(savedInstrument.getHashPanChildren());
    }

    @Test
    void whenTkmDeleteTokensThenPaymentInstrumentShouldDeleteIt() {
      final var hashPan = TestUtils.generateRandomHashPan().getValue();
      final var tokenCommands = TestUtils.generateRandomUpdateTokenCommand(10);
      final var tokenToBeDeleted = tokenCommands.stream()
              .filter(it -> it.getAction() == TkmUpdateCommand.TkmTokenCommand.Action.DELETE)
              .map(it -> HashPan.create(it.getHashPan()))
              .collect(Collectors.toSet());

      final var fakePaymentInstrument = EnrolledPaymentInstrument.create(HashPan.create(hashPan), SourceApp.ID_PAY);
      tokenToBeDeleted.forEach(fakePaymentInstrument::addHashPanChild);
      Mockito.when(repository.findByHashPan(hashPan)).thenReturn(Optional.of(fakePaymentInstrument));

      final var updateCommand = new TkmUpdateCommand(hashPan, null, tokenCommands);

      service.handle(updateCommand);
      Mockito.verify(repository).save(paymentInstrumentArgumentCaptor.capture());

      final var savedInstrument = paymentInstrumentArgumentCaptor.getValue();
      assertTrue(savedInstrument.isReady());
      assertThat(savedInstrument.getHashPanChildren()).doesNotContainAnyElementsOf(tokenToBeDeleted);
    }

    @Test
    void whenTkmUpdateParThenPaymentInstrumentUpdateIt() {
      final var hashPan = TestUtils.generateRandomHashPan();
      final var updateCommand = new TkmUpdateCommand(hashPan.getValue(), "par", List.of());
      Mockito.when(repository.findByHashPan(hashPan.getValue())).thenReturn(
              Optional.of(EnrolledPaymentInstrument.create(hashPan, Set.of(SourceApp.ID_PAY), "", ""))
      );

      service.handle(updateCommand);
      Mockito.verify(repository).save(paymentInstrumentArgumentCaptor.capture());
      assertEquals("par", paymentInstrumentArgumentCaptor.getValue().getPar());
    }

    @Test
    void whenUpdateCommandMissMandatoryFieldsThenThrowAnException() {
      final var invalidUpdateCommands = List.of(
              new TkmUpdateCommand(null, "", List.of()),
              new TkmUpdateCommand(TestUtils.generateRandomHashPan().getValue(), "", List.of(
                      new TkmUpdateCommand.TkmTokenCommand(null, null)
              ))
      );

      assertTrue(invalidUpdateCommands.stream().noneMatch(command -> {
        try {
          service.handle(command);
          return true;
        } catch (ConstraintViolationException | IllegalArgumentException t) {
          return false;
        }
      }));
    }
  }


  @Nested
  @DisplayName("Tests for revoke command")
  class RevokeCommandCases {

    @Test
    void whenTkmRevokeThenPaymentInstrumentIsRevoked() {
      final var hashPan = TestUtils.generateRandomHashPan();
      Mockito.when(repository.findByHashPan(hashPan.getValue())).thenReturn(
              Optional.of(EnrolledPaymentInstrument.create(hashPan, SourceApp.ID_PAY))
      );
      final var revokeCommand = new TkmRevokeCommand("taxCode", hashPan.getValue(), "par");

      service.handle(revokeCommand);
      Mockito.verify(repository).save(paymentInstrumentArgumentCaptor.capture());
      assertTrue(paymentInstrumentArgumentCaptor.getValue().isRevoked());
    }

    @Test
    void whenTkmRevokeOnAlreadyRevokedInstrumentThenDoNothing() { // idempotency
      final var hashPan = TestUtils.generateRandomHashPan();
      final var mockedInstrument = EnrolledPaymentInstrument.create(hashPan, SourceApp.ID_PAY);
      final var revokeCommand = new TkmRevokeCommand("taxCode", hashPan.getValue(), "par");

      mockedInstrument.revokeInstrument();

      Mockito.when(repository.findByHashPan(hashPan.getValue())).thenReturn(Optional.of(mockedInstrument));

      service.handle(revokeCommand);
      Mockito.verify(repository).save(paymentInstrumentArgumentCaptor.capture());
      assertTrue(paymentInstrumentArgumentCaptor.getValue().isRevoked());
    }

    @Test
    void whenPaymentInstrumentIsRevokedThenNotifyToDownstream() {
      final var hashPan = TestUtils.generateRandomHashPan();
      Mockito.when(repository.findByHashPan(hashPan.getValue())).thenReturn(
              Optional.of(EnrolledPaymentInstrument.create(hashPan, Set.of(SourceApp.ID_PAY), "", ""))
      );
      final var revokeCommand = new TkmRevokeCommand("taxCode", hashPan.getValue(), "par");

      service.handle(revokeCommand);

      Mockito.verify(revokeService, Mockito.times(1)).notifyRevoke(Set.of(SourceApp.ID_PAY), "taxCode", hashPan);
    }

    @Test
    void whenTkmRevokeOnNonExistingCardThenOnlyNotifyToDownstream() {
      final var hashPan = TestUtils.generateRandomHashPan();
      final var revokeCommand = new TkmRevokeCommand("taxCode", hashPan.getValue(), "par");

      Mockito.when(repository.findByHashPan(hashPan.getValue())).thenReturn(Optional.empty());

      service.handle(revokeCommand);

      Mockito.verify(repository, Mockito.times(0)).save(any());
      Mockito.verify(revokeService, Mockito.times(1)).notifyRevoke(Set.of(), "taxCode", hashPan);
    }

    @Test
    void whenRevokeDownstreamNotificationFailsThenThrowAnException() {
      final var hashPan = TestUtils.generateRandomHashPan();
      Mockito.when(repository.findByHashPan(hashPan.getValue())).thenReturn(
              Optional.of(EnrolledPaymentInstrument.create(hashPan, Set.of(SourceApp.ID_PAY), "", ""))
      );
      final var revokeCommand = new TkmRevokeCommand("taxCode", hashPan.getValue(), "par");
      doReturn(false).when(revokeService).notifyRevoke(any(), any(), any());

      assertThrowsExactly(FailedToNotifyRevoke.class, () -> service.handle(revokeCommand));

      Mockito.verify(repository, Mockito.times(1)).save(any());
      Mockito.verify(revokeService, Mockito.times(1)).notifyRevoke(Set.of(SourceApp.ID_PAY),"taxCode", hashPan);
    }

    @Test
    void whenRevokeCommandMissMandatoryFieldsThenThrowAnException() {
      final var invalidUpdateCommands = List.of(
              new TkmRevokeCommand(null, "123", ""),
              new TkmRevokeCommand("", "", ""),
              new TkmRevokeCommand("123", null, ""),
              new TkmRevokeCommand(null, null, "")
      );

      assertTrue(invalidUpdateCommands.stream().noneMatch(command -> {
        try {
          service.handle(command);
          return true;
        } catch (ConstraintViolationException | IllegalArgumentException t) {
          return false;
        }
      }));
    }
  }

  @TestConfiguration
  @Import({DomainEventPublisher.class, EnrolledPaymentInstrumentEventListener.class})
  static class Config {

    @MockBean
    EnrolledPaymentInstrumentRepository repository;

    @MockBean
    InstrumentRevokeNotificationService revokeNotificationService;

    @Bean
    EnrollNotifyService enrollNotifyService() {
      return new EnrollNotifyService() {
        @Override
        public boolean confirmEnroll(SourceApp app, HashPan hashPan, Date enrollDate) {
          return true;
        }

        @Override
        public boolean confirmExport(HashPan hashPan, Date at) {
          return true;
        }
      };
    }

    @Bean
    TkmPaymentInstrumentService service(@Autowired DomainEventPublisher domainEventPublisher) {
      return new TkmPaymentInstrumentService(repository, revokeNotificationService, domainEventPublisher);
    }
  }
}
