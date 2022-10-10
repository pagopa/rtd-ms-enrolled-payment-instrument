package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrustructure.persistence;


import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.EnrolledPaymentInstrumentEntity;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("mongo-integration-test")
@TestPropertySource(properties = {
        "spring.config.location=classpath:application-test.yml"}, inheritProperties = false)
@AutoConfigureDataMongo
class EnrolledPaymentInstrumentRepositoryTest {

  private static final HashPan TEST_HASH_PAN = HashPan.create(
          "4971175b7c192c7eda18d8c4a1fbb30372333445c5b6c5ef738b333a2729a266");

  @Resource
  private EnrolledPaymentInstrumentRepositoryImpl repository;

  @BeforeEach
  void setup(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.indexOps("enrolled_payment_instrument")
            .ensureIndex(new Index().on("hashPan", Direction.ASC).unique());
  }

  @AfterEach
  void clean(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.dropCollection(EnrolledPaymentInstrumentEntity.class);
  }

  @Test
  void shouldSaveDomainToRightDocument() {
    final var instrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, Set.of(SourceApp.values()), null, null);

    repository.save(instrument);

    final var savedDomain = repository.findByHashPan(TEST_HASH_PAN.getValue()).orElseThrow();

    assertEquals(TEST_HASH_PAN, savedDomain.getHashPan());
    assertEquals(Set.of(SourceApp.values()), savedDomain.getEnabledApps());
  }

  @Test
  void shouldDeleteRightDocument() {
    final var instrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, Set.of(), null, null);
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
}
