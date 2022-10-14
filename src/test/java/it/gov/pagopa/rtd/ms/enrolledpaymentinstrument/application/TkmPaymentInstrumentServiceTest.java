package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.ApplicationTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import javax.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(ApplicationTestConfiguration.class)
@EnableAutoConfiguration(exclude = {EmbeddedMongoAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class TkmPaymentInstrumentServiceTest {

  @Autowired
  private EnrolledPaymentInstrumentRepository repository;

  @Autowired
  private TkmPaymentInstrumentService service;

  private ArgumentCaptor<EnrolledPaymentInstrument> paymentInstrumentArgumentCaptor;

  @BeforeEach
  void setup() {
    paymentInstrumentArgumentCaptor = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    Mockito.reset(repository);
  }

  @Nested
  @DisplayName("Tests for update command")
  class UpdateCommandCases {
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

    @Autowired
    private InstrumentRevokeNotificationService revokeService;

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
              Optional.of(EnrolledPaymentInstrument.create(hashPan, Set.of(), "", ""))
      );
      final var revokeCommand = new TkmRevokeCommand("taxCode", hashPan.getValue(), "par");

      service.handle(revokeCommand);

      Mockito.verify(revokeService, Mockito.times(1)).notifyRevoke("taxCode", hashPan);
    }

    @Test
    void whenHandleRevokeOnNonExistingCardThenOnlyNotifyToDownstream() {
      final var hashPan = TestUtils.generateRandomHashPan();
      final var revokeCommand = new TkmRevokeCommand("taxCode", hashPan.getValue(), "par");

      Mockito.when(repository.findByHashPan(hashPan.getValue())).thenReturn(Optional.empty());

      service.handle(revokeCommand);

      Mockito.verify(repository, Mockito.times(0)).save(Mockito.any());
      Mockito.verify(revokeService, Mockito.times(1)).notifyRevoke("taxCode", hashPan);
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
}
