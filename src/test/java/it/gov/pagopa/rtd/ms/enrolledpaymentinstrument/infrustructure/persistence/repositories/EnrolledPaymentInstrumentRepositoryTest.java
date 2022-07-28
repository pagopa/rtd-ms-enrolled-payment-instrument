package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrustructure.persistence.repositories;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.mongo.MongoConfig;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.persistence.repositories.EnrolledPaymentInstrumentRepositoryImpl;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("mongo-integration-test")
@TestPropertySource(properties = {
    "spring.config.location=classpath:application-test.yml"}, inheritProperties = false)
@Import(value = MongoConfig.class)
@AutoConfigureBefore(EmbeddedMongoAutoConfiguration.class)
@EnableConfigurationProperties({MongoProperties.class, EmbeddedMongoProperties.class})
public class EnrolledPaymentInstrumentRepositoryTest {

  private static final HashPan TEST_HASH_PAN = HashPan.create(
      "4971175b7c192c7eda18d8c4a1fbb30372333445c5b6c5ef738b333a2729a266");

  @Resource
  private EnrolledPaymentInstrumentRepositoryImpl repository;

  private final ExecutorService executor = Executors.newFixedThreadPool(3);

  @BeforeAll
  static void setupAll(@Autowired MongoTemplate mongoTemplate) {
    mongoTemplate.indexOps("enrolled_payment_instrument")
        .ensureIndex(new Index().on("hashPan", Direction.ASC).unique());
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
/*
  @Test
  void concurrencyWriteMustThrowWriteConflict() {
    final var instruments = Arrays.stream(SourceApp.values()).map(app ->
        EnrolledPaymentInstrument.create(TEST_HASH_PAN, app, null, null)
    ).collect(Collectors.toList());

    final var futures = instruments.stream().map(instrument ->
        executor.submit(() -> saveTransactionally(instrument))
    );

    final var writeConflicts = futures.map(future -> {
      try {
        future.get(5, TimeUnit.SECONDS);
        return false;
      } catch (WriteConflict e) {
        e.printStackTrace();
        return true;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    }).collect(Collectors.toList());

    final var aa = repository.findByHashPan(TEST_HASH_PAN.getValue());
    assertTrue(true);
    //assertEquals(instruments.size() - 1, writeConflicts.stream().filter(i -> i).count());
  }

  @Transactional
  private void saveTransactionally(EnrolledPaymentInstrument instrument) {
    delayConditionally(2000, () -> instrument.getEnabledApps().contains(SourceApp.ID_PAY));
    repository.save(instrument);
  }

  private void delayConditionally(int delay, Supplier<Boolean> supplier) {
    if (supplier.get()) {
      delay(delay);
    }
  }

  private void delay(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }*/
}
