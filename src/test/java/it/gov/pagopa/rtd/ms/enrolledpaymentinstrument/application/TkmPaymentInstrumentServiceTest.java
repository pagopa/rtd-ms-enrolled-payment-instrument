package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.FailedToNotifyRevoke;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.errors.VirtualEnrollError;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.EnrollAckService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.VirtualEnrollService;
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

import javax.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
  private VirtualEnrollService virtualEnrollService;

  @Autowired
  private TkmPaymentInstrumentService service;

  private ArgumentCaptor<EnrolledPaymentInstrument> paymentInstrumentArgumentCaptor;

  @BeforeEach
  void setUp() {
    paymentInstrumentArgumentCaptor = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    doReturn(true).when(revokeService).notifyRevoke(any(), any(), any());
    doReturn(true).when(virtualEnrollService).enroll(any(), any(), any());
  }

  @AfterEach()
  void clean() {
    Mockito.reset(repository, revokeService, virtualEnrollService);
  }


  @Nested
  @DisplayName("Tests for update command")
  class UpdateCommandCases {

    @BeforeEach
    void setup() {
      doReturn(true).when(virtualEnrollService).enroll(any(), any(), any());
      doReturn(true).when(virtualEnrollService).enrollToken(any(), any(), any(), any());
    }

    @Test
    void whenHandleFirstTkmUpdateCommandThenPaymentInstrumentIsNotEnrolled() {
      final var updateCommand = new TkmUpdateCommand(
              TestUtils.generateRandomHashPan().getValue(),
              null,
              Collections.emptyList()
      );
      service.handle(updateCommand);
      Mockito.verify(repository).save(paymentInstrumentArgumentCaptor.capture());

      assertEquals(updateCommand.getHashPan(), paymentInstrumentArgumentCaptor.getValue().getHashPan().getValue());
      assertNull(paymentInstrumentArgumentCaptor.getValue().getPar());
      assertTrue(paymentInstrumentArgumentCaptor.getValue().isNotEnrolled());
    }

    @Test
    void whenHandleUpdateWithNewTokensThenPaymentInstrumentShouldAddIt() {
      final var tokenCommands = TestUtils.generateRandomUpdateTokenCommand(10);
      final var tokenToBeInsert = tokenCommands
              .stream()
              .filter(it -> it.getAction() == TkmUpdateCommand.TkmTokenCommand.Action.UPDATE)
              .map(it -> HashPan.create(it.getHashPan()))
              .collect(Collectors.toSet());
      final var updateCommand = new TkmUpdateCommand(
              TestUtils.generateRandomHashPan().getValue(),
              null,
              tokenCommands
      );
      service.handle(updateCommand);
      Mockito.verify(repository).save(paymentInstrumentArgumentCaptor.capture());

      final var savedInstrument = paymentInstrumentArgumentCaptor.getValue();
      assertTrue(savedInstrument.isNotEnrolled());
      assertThat(tokenToBeInsert).hasSameElementsAs(savedInstrument.getHashPanChildren());
    }

    @Test
    void whenHandleUpdateWithDeletedTokensThenPaymentInstrumentShouldDeleteIt() {
      final var hashPan = TestUtils.generateRandomHashPan().getValue();
      final var tokenCommands = TestUtils.generateRandomUpdateTokenCommand(10);
      final var tokenToBeDeleted = tokenCommands.stream()
              .filter(it -> it.getAction() == TkmUpdateCommand.TkmTokenCommand.Action.DELETE)
              .map(it -> HashPan.create(it.getHashPan()))
              .collect(Collectors.toSet());

      final var fakePaymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(HashPan.create(hashPan), "", "");
      tokenToBeDeleted.forEach(fakePaymentInstrument::addHashPanChild);
      Mockito.when(repository.findByHashPan(hashPan)).thenReturn(Optional.of(fakePaymentInstrument));

      final var updateCommand = new TkmUpdateCommand(hashPan, null, tokenCommands);

      service.handle(updateCommand);
      Mockito.verify(repository).save(paymentInstrumentArgumentCaptor.capture());

      final var savedInstrument = paymentInstrumentArgumentCaptor.getValue();
      assertTrue(savedInstrument.isNotEnrolled());
      assertThat(savedInstrument.getHashPanChildren()).doesNotContainAnyElementsOf(tokenToBeDeleted);
    }

    @Test
    void whenHandleUpdateWithParThenPaymentInstrumentUpdateIt() {
      final var updateCommand = new TkmUpdateCommand(TestUtils.generateRandomHashPan().getValue(), "par", List.of());

      service.handle(updateCommand);
      Mockito.verify(repository).save(paymentInstrumentArgumentCaptor.capture());
      assertEquals("par", paymentInstrumentArgumentCaptor.getValue().getPar());
    }

    @Test
    void whenCommandUpdateParThenDoVirtualEnroll() {
      final var hashPan = TestUtils.generateRandomHashPan();
      final var updateCommand = new TkmUpdateCommand(hashPan.getValue(), "par", List.of());
      doReturn(true).when(virtualEnrollService).enroll(hashPan, "par", Set.of());

      service.handle(updateCommand);

      Mockito.verify(repository, times(1)).save(any());
      Mockito.verify(virtualEnrollService, times(1)).enroll(hashPan, "par", Set.of());
    }

    @Test
    void whenCommandUpdateTheSameParThenAvoidVirtualEnroll() {
      final var paymentInstrument = EnrolledPaymentInstrument.create(
              TestUtils.generateRandomHashPan(),
              Set.of(SourceApp.ID_PAY),
              "", ""
      );
      paymentInstrument.associatePar("par");
      paymentInstrument.clearDomainEvents();

      final var updateCommand = new TkmUpdateCommand(paymentInstrument.getHashPan().getValue(), "par", List.of());
      doReturn(Optional.of(paymentInstrument)).when(repository).findByHashPan(any());

      service.handle(updateCommand);

      Mockito.verify(repository, times(1)).save(any());
      Mockito.verify(virtualEnrollService, times(0)).enroll(any(), any(), eq(Set.of(SourceApp.ID_PAY)));
    }

    @Test
    void whenCommandDoesntUpdateParThenAvoidVirtualEnroll() {
      final var hashPan = TestUtils.generateRandomHashPan();
      final var updateCommand = new TkmUpdateCommand(hashPan.getValue(), null, List.of());
      service.handle(updateCommand);

      Mockito.verify(repository, times(1)).save(any());
      Mockito.verify(virtualEnrollService, times(0)).enroll(any(), any(), any());
    }

    @Test
    void whenVirtualEnrollFailsThenThrowAnError() {
      final var hashPan = TestUtils.generateRandomHashPan();
      final var updateCommand = new TkmUpdateCommand(hashPan.getValue(), "par", List.of());
      doReturn(false).when(virtualEnrollService).enroll(any(), any(), any());

      assertThrowsExactly(VirtualEnrollError.class, () -> service.handle(updateCommand));
    }

    @Test
    void whenUpdateHashTokenOnReadyInstrumentThenDoVirtualEnroll() {
      final var hashPanCaptor = ArgumentCaptor.forClass(HashPan.class);
      final var hashTokenCaptor = ArgumentCaptor.forClass(HashPan.class);
      final ArgumentCaptor<Set<SourceApp>> appsCaptor = ArgumentCaptor.forClass(Set.class);
      final var paymentInstrument = EnrolledPaymentInstrument.create(
              TestUtils.generateRandomHashPan(),
              Set.of(SourceApp.ID_PAY),
              "", ""
      );
      final var hashToken = TestUtils.generateRandomHashPan();
      final var updateCommand = new TkmUpdateCommand(paymentInstrument.getHashPan().getValue(), "par",
              List.of(new TkmUpdateCommand.TkmTokenCommand(hashToken.getValue(), TkmUpdateCommand.TkmTokenCommand.Action.UPDATE)));

      doReturn(Optional.of(paymentInstrument)).when(repository).findByHashPan(any());

      service.handle(updateCommand);

      Mockito.verify(repository, times(1)).save(any());
      Mockito.verify(virtualEnrollService, times(1)).enroll(hashPanCaptor.capture(), any(), appsCaptor.capture());
      Mockito.verify(virtualEnrollService, times(1)).enrollToken(hashPanCaptor.capture(), hashTokenCaptor.capture(), any(), appsCaptor.capture());

      assertThat(hashPanCaptor.getAllValues()).hasSameElementsAs(List.of(paymentInstrument.getHashPan(), paymentInstrument.getHashPan()));
      assertThat(hashTokenCaptor.getValue()).isEqualTo(hashToken);
      assertThat(appsCaptor.getAllValues()).allSatisfy(it -> assertThat(it).hasSameElementsAs(Set.of(SourceApp.ID_PAY)));
    }

    @Test
    void whenUpdateHashTokenOnNotReadyInstrumentThenAvoidVirtualEnroll() {
      final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(TestUtils.generateRandomHashPan());
      final var hashToken = TestUtils.generateRandomHashPan();
      final var updateCommand = new TkmUpdateCommand(paymentInstrument.getHashPan().getValue(), null,
              List.of(new TkmUpdateCommand.TkmTokenCommand(hashToken.getValue(), TkmUpdateCommand.TkmTokenCommand.Action.UPDATE)));

      doReturn(Optional.of(paymentInstrument)).when(repository).findByHashPan(any());

      service.handle(updateCommand);
      Mockito.verify(virtualEnrollService, times(0)).enrollToken(any(), any(), any(), any());
    }

    @Test
    void whenUpdateExistingHashTokenOnReadyInstrumentThenAvoidVirtualEnroll() {
      final var hashPan = TestUtils.generateRandomHashPan();
      final var hashToken = TestUtils.generateRandomHashPan();
      final var mockInstrument = EnrolledPaymentInstrument.create(hashPan, Set.of(SourceApp.ID_PAY), "", "");
      final var updateCommand = new TkmUpdateCommand(hashPan.getValue(), null,
              List.of(new TkmUpdateCommand.TkmTokenCommand(hashToken.getValue(), TkmUpdateCommand.TkmTokenCommand.Action.UPDATE)));

      mockInstrument.addHashPanChild(hashToken);
      mockInstrument.clearDomainEvents();
      doReturn(Optional.of(mockInstrument)).when(repository).findByHashPan(any());
      doReturn(true).when(virtualEnrollService).enrollToken(any(), any(), any(), any());

      service.handle(updateCommand);

      Mockito.verify(repository, times(1)).save(any());
      Mockito.verify(virtualEnrollService, times(0)).enrollToken(any(), any(), any(), any());
    }

    @Test
    void whenDeleteHashTokenOnReadyInstrumentThenDoVirtualUnEnroll() {
      final var paymentInstrument = EnrolledPaymentInstrument.create(TestUtils.generateRandomHashPan(), Set.of(SourceApp.ID_PAY), "", "");
      final var hashToken = TestUtils.generateRandomHashPan();
      final var updateCommand = new TkmUpdateCommand(paymentInstrument.getHashPan().getValue(), null,
              List.of(new TkmUpdateCommand.TkmTokenCommand(hashToken.getValue(), TkmUpdateCommand.TkmTokenCommand.Action.DELETE)));

      paymentInstrument.addHashPanChild(hashToken);
      doReturn(Optional.of(paymentInstrument)).when(repository).findByHashPan(any());
      doReturn(true).when(virtualEnrollService).unEnrollToken(any(), any(), any(), any());

      service.handle(updateCommand);
      Mockito.verify(virtualEnrollService, times(1)).unEnrollToken(
              eq(paymentInstrument.getHashPan()), eq(hashToken), any(), eq(Set.of(SourceApp.ID_PAY)));
    }

    @Test
    void whenDeleteHashTokenOnNotReadyInstrumentThenAvoidVirtualUnEnroll() {
      final var paymentInstrument = EnrolledPaymentInstrument.createUnEnrolledInstrument(TestUtils.generateRandomHashPan());
      final var hashToken = TestUtils.generateRandomHashPan();
      final var updateCommand = new TkmUpdateCommand(paymentInstrument.getHashPan().getValue(), null,
              List.of(new TkmUpdateCommand.TkmTokenCommand(hashToken.getValue(), TkmUpdateCommand.TkmTokenCommand.Action.UPDATE)));

      paymentInstrument.addHashPanChild(hashToken);
      doReturn(Optional.of(paymentInstrument)).when(repository).findByHashPan(any());
      doReturn(true).when(virtualEnrollService).unEnrollToken(any(), any(), any(), any());

      service.handle(updateCommand);
      Mockito.verify(virtualEnrollService, times(0)).unEnrollToken(any(), any(), any(), any());
    }

    @Test
    void whenDeleteNonExistingHashTokenOnReadyInstrumentThenAvoidVirtualEnroll() {
      final var paymentInstrument = EnrolledPaymentInstrument.create(TestUtils.generateRandomHashPan(), Set.of(SourceApp.ID_PAY), "", "");
      final var hashToken = TestUtils.generateRandomHashPan();
      final var updateCommand = new TkmUpdateCommand(paymentInstrument.getHashPan().getValue(), null,
              List.of(new TkmUpdateCommand.TkmTokenCommand(hashToken.getValue(), TkmUpdateCommand.TkmTokenCommand.Action.UPDATE)));

      doReturn(Optional.of(paymentInstrument)).when(repository).findByHashPan(any());
      doReturn(true).when(virtualEnrollService).unEnrollToken(any(), any(), any(), any());

      service.handle(updateCommand);
      Mockito.verify(virtualEnrollService, times(0)).unEnrollToken(any(), any(), any(), any());
    }

    @Test
    void whenVirtualUnEnrollFailsThenThrowAnError() {
      final var paymentInstrument = EnrolledPaymentInstrument.create(TestUtils.generateRandomHashPan(), Set.of(SourceApp.ID_PAY), "", "");
      final var hashToken = TestUtils.generateRandomHashPan();
      final var updateCommand = new TkmUpdateCommand(paymentInstrument.getHashPan().getValue(), "par",
              List.of(new TkmUpdateCommand.TkmTokenCommand(hashToken.getValue(), TkmUpdateCommand.TkmTokenCommand.Action.DELETE)));
      paymentInstrument.addHashPanChild(hashToken);
      doReturn(Optional.of(paymentInstrument)).when(repository).findByHashPan(any());
      doReturn(false).when(virtualEnrollService).unEnrollToken(any(), any(), any(), any());

      assertThrowsExactly(VirtualEnrollError.class, () -> service.handle(updateCommand));
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
    void whenHandleRevokeThenPaymentInstrumentIsRevoked() {
      final var hashPan = TestUtils.generateRandomHashPan();
      Mockito.when(repository.findByHashPan(hashPan.getValue())).thenReturn(
              Optional.of(EnrolledPaymentInstrument.create(hashPan, Set.of(), "", ""))
      );
      final var revokeCommand = new TkmRevokeCommand("taxCode", hashPan.getValue(), "par");

      service.handle(revokeCommand);
      Mockito.verify(repository).save(paymentInstrumentArgumentCaptor.capture());
      assertTrue(paymentInstrumentArgumentCaptor.getValue().isRevoked());
    }

    @Test
    void whenHandleRevokeOnAlreadyRevokedInstrumentThenDoNothing() { // idempotency
      final var hashPan = TestUtils.generateRandomHashPan();
      final var mockedInstrument = EnrolledPaymentInstrument.create(hashPan, Set.of(), "", "");
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

      Mockito.verify(revokeService, times(1)).notifyRevoke(Set.of(SourceApp.ID_PAY), "taxCode", hashPan);
    }

    @Test
    void whenHandleRevokeOnNonExistingCardThenOnlyNotifyToDownstream() {
      final var hashPan = TestUtils.generateRandomHashPan();
      final var revokeCommand = new TkmRevokeCommand("taxCode", hashPan.getValue(), "par");

      Mockito.when(repository.findByHashPan(hashPan.getValue())).thenReturn(Optional.empty());

      service.handle(revokeCommand);

      Mockito.verify(repository, times(0)).save(any());
      Mockito.verify(revokeService, times(1)).notifyRevoke(Set.of(), "taxCode", hashPan);
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

      Mockito.verify(repository, times(1)).save(any());
      Mockito.verify(revokeService, times(1)).notifyRevoke(Set.of(SourceApp.ID_PAY),"taxCode", hashPan);
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
    EnrollAckService enrollAckService() {
      return (app, hashPan, enrollDate) -> true;
    }

    @MockBean
    VirtualEnrollService virtualEnrollService;

    @Bean
    TkmPaymentInstrumentService service(@Autowired DomainEventPublisher domainEventPublisher) {
      return new TkmPaymentInstrumentService(repository, revokeNotificationService, domainEventPublisher);
    }
  }
}
