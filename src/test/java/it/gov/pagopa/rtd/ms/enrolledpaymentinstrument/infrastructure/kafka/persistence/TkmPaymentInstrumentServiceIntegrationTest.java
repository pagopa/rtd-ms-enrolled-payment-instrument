package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.persistence;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.TkmPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmRevokeCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.TkmUpdateCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.MongodbIntegrationTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.VirtualEnrollService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.EnrolledPaymentInstrumentEntity;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("mongo-integration-test")
@TestPropertySource(properties = {
        "spring.config.location=classpath:application-test.yml"}, inheritProperties = false)
@Import({ MongodbIntegrationTestConfiguration.class })
@AutoConfigureDataMongo
public class TkmPaymentInstrumentServiceIntegrationTest {

  @MockBean
  private InstrumentRevokeNotificationService notificationService;

  @MockBean
  private VirtualEnrollService virtualEnrollService;

  @Autowired
  private EnrolledPaymentInstrumentRepositoryImpl repository;

  private TkmPaymentInstrumentService paymentInstrumentService;

  @BeforeEach
  void setup(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.indexOps("enrolled_payment_instrument")
            .ensureIndex(new Index().on("hashPan", Sort.Direction.ASC).unique());
    this.paymentInstrumentService = new TkmPaymentInstrumentService(repository, notificationService, virtualEnrollService);
  }

  @AfterEach
  void teardown(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.dropCollection(EnrolledPaymentInstrumentEntity.class);
  }

  @Test
  void whenUpdateNonExistingInstrumentThenNonEnrolledInstrumentIsCreated() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var command = new TkmUpdateCommand(hashPan.getValue(), "par", List.of());

    paymentInstrumentService.handle(command);

    final var savedInstrument = repository.findByHashPan(hashPan.getValue()).get();

    assertTrue(savedInstrument.isNotEnrolled());
  }

  @Test
  void whenUpdateTokensThenMustBeFound() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var tokenCommands = TestUtils.generateRandomUpdateTokenCommand(10);
    final var command = new TkmUpdateCommand(hashPan.getValue(), "par", tokenCommands);

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
    paymentInstrumentService.handle(new TkmUpdateCommand(hashPan.getValue(), "", List.of()));

    paymentInstrumentService.handle(new TkmRevokeCommand("taxCode", hashPan.getValue(), ""));

    assertTrue(repository.findByHashPan(hashPan.getValue()).get().isRevoked());
  }
}
