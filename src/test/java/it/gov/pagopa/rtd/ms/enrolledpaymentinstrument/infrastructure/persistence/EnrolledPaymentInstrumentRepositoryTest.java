package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence;


import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.MongoDbTest;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.RepositoryConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.EnrolledPaymentInstrumentEntity;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentDao;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@MongoDbTest
@Import(RepositoryConfiguration.class)
class EnrolledPaymentInstrumentRepositoryTest {

  private static final HashPan TEST_HASH_PAN = TestUtils.generateRandomHashPan();
  private static final HashPan TEST_CHILD_HASH_PAN = TestUtils.generateRandomHashPan();
  private static final String TEST_PAR = "par";

  @Autowired
  private EnrolledPaymentInstrumentDao dao;
  private EnrolledPaymentInstrumentRepository repository;

  @BeforeEach
  void setup(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.indexOps("enrolled_payment_instrument")
            .ensureIndex(new Index().on("hashPan", Direction.ASC).unique());

    repository = new EnrolledPaymentInstrumentRepositoryImpl(dao);
  }

  @AfterEach
  void clean(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.dropCollection(EnrolledPaymentInstrumentEntity.class);
  }

  @Test
  void shouldSaveDomainToRightDocument() {
    final var instrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, Set.of(SourceApp.values()), null, null);
    instrument.addHashPanChild(TEST_CHILD_HASH_PAN);
    instrument.associatePar(TEST_PAR);

    repository.save(instrument);

    final var savedDomain = repository.findByHashPan(TEST_HASH_PAN.getValue()).orElseThrow();

    assertEquals(TEST_HASH_PAN, savedDomain.getHashPan());
    assertEquals(Set.of(SourceApp.values()), savedDomain.getEnabledApps());
    assertEquals(Set.of(TEST_CHILD_HASH_PAN), savedDomain.getHashPanChildren());
    assertEquals(TEST_PAR, savedDomain.getPar());
    assertTrue(instrument.isReady());
  }

  @Test
  void shouldDeleteRightDocument() {
    final var instrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, SourceApp.ID_PAY, null, null);
    repository.delete(instrument);
    assertTrue(repository.findByHashPan(TEST_HASH_PAN.getValue()).isEmpty());
  }

  @Test
  void shouldRaiseDuplicateKeyWhenTryToSaveNewPaymentInstrumentWithSameHashPan() {
    final var instrument = List.of(
            EnrolledPaymentInstrument.create(TEST_HASH_PAN, Set.of(SourceApp.values()), null, null),
            EnrolledPaymentInstrument.create(TEST_HASH_PAN, Set.of(SourceApp.values()), null, null)
    );
    repository.save(instrument.get(0));

    assertThrowsExactly(DuplicateKeyException.class, () -> repository.save(instrument.get(1)));
  }

  @Test
  void shouldRaiseOptimisticLockWhenTryToSaveConcurrently() {
    repository.save(EnrolledPaymentInstrument.create(TEST_HASH_PAN, Set.of(SourceApp.values()), null, null));

    final var instrument1 = repository.findByHashPan(TEST_HASH_PAN.getValue()).get();
    final var instrument2 = repository.findByHashPan(TEST_HASH_PAN.getValue()).get();

    instrument1.disableApp(SourceApp.FA);
    instrument2.enableApp(SourceApp.FA);

    repository.save(instrument1);
    assertThrowsExactly(OptimisticLockingFailureException.class, () -> repository.save(instrument2));
  }

  @Test
  void mustSaveExportHashPans() {
      final var childHashPan = TestUtils.generateRandomHashPan();
      final var instrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, SourceApp.ID_PAY, null, null);
      instrument.addHashPanChild(childHashPan);

      repository.save(instrument);
      assertThat(dao.findByHashPan(TEST_HASH_PAN.getValue()).orElseThrow().getHashPanExports())
              .hasSameElementsAs(List.of(childHashPan, TEST_HASH_PAN));
  }
}
