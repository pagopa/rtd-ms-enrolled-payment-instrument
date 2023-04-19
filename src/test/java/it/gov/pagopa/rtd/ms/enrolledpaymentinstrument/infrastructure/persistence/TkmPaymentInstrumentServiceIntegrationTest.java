package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.TkmPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.DomainEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.RepositoryConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.EnrolledPaymentInstrumentEntity;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringRunner.class)
@DataMongoTest
@Testcontainers
@Import(RepositoryConfiguration.class)
class TkmPaymentInstrumentServiceIntegrationTest {

  @MockBean
  private InstrumentRevokeNotificationService notificationService;

  @MockBean
  private DomainEventPublisher domainEventPublisher;

  @Autowired
  private EnrolledPaymentInstrumentRepositoryImpl repository;

  private TkmPaymentInstrumentService paymentInstrumentService;

  @BeforeEach
  void setup(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.indexOps("enrolled_payment_instrument")
            .ensureIndex(new Index().on("hashPan", Sort.Direction.ASC).unique());
    doReturn(true).when(notificationService).notifyRevoke(any(), any(), any());
    this.paymentInstrumentService = new TkmPaymentInstrumentService(repository, notificationService, domainEventPublisher);

  }

  @AfterEach
  void teardown(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.dropCollection(EnrolledPaymentInstrumentEntity.class);
  }

  @Test
  void whenUpdateNonExistingInstrumentThenNonEnrolledInstrumentIsCreated() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var paymentInstrument = EnrolledPaymentInstrument.create(hashPan, Set.of(SourceApp.ID_PAY), "", "");
    final var command = new TkmUpdateCommand(hashPan.getValue(), "par", List.of());

    repository.save(paymentInstrument);
    paymentInstrumentService.handle(command);

    final var savedInstrument = repository.findByHashPan(hashPan.getValue()).get();

    assertTrue(savedInstrument.isReady());
  }

  @Test
  void whenUpdateTokensThenMustBeFound() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var paymentInstrument = EnrolledPaymentInstrument.create(hashPan, Set.of(SourceApp.ID_PAY), "", "");
    final var tokenCommands = TestUtils.generateRandomUpdateTokenCommand(10);
    final var command = new TkmUpdateCommand(hashPan.getValue(), "par", tokenCommands);

    repository.save(paymentInstrument);
    paymentInstrumentService.handle(command);
    final var savedInstrument = repository.findByHashPan(hashPan.getValue()).get();
    final var newTokens = tokenCommands.stream()
            .filter(it -> it.getAction() == TkmUpdateCommand.TkmTokenCommand.Action.UPDATE)
            .map(it -> HashPan.create(it.getHashPan())).collect(Collectors.toSet());
    assertThat(savedInstrument.getHashPanChildren()).hasSameElementsAs(newTokens);
  }

  @Test
  void whenCardIsRevokedThenMustBeSavedAsRevoked() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var paymentInstrument = EnrolledPaymentInstrument.create(hashPan, Set.of(SourceApp.ID_PAY), "", "");
    repository.save(paymentInstrument);

    paymentInstrumentService.handle(new TkmUpdateCommand(hashPan.getValue(), "", List.of()));

    paymentInstrumentService.handle(new TkmRevokeCommand("taxCode", hashPan.getValue(), ""));

    assertTrue(repository.findByHashPan(hashPan.getValue()).get().isRevoked());
  }

  @Container
  public static final MongoDBContainer mongoContainer = new MongoDBContainer("mongo:4.4.4");

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
  }
}
