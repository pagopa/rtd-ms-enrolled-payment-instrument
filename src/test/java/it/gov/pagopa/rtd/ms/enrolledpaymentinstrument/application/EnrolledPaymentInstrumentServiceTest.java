package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.EnrollPaymentInstrumentCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.EnrolledPaymentInstrument;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.repositories.EnrolledPaymentInstrumentRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.config.location=classpath:application-test.yml" }, inheritProperties = false)
public class EnrolledPaymentInstrumentServiceTest {

  private static final HashPan TEST_HASH_PAN = HashPan.create("4971175b7c192c7eda18d8c4a1fbb30372333445c5b6c5ef738b333a2729a266");

  @MockBean
  private EnrolledPaymentInstrumentRepository repository;

  @Autowired
  private EnrolledPaymentInstrumentService service;

  @BeforeEach
  void setup() {
    Mockito.reset(repository);
  }

  @DisplayName("must enable payment instrument for a specific source app")
  @Test
  public void mustEnablePaymentInstrumentForSpecificApp() {
    final var argument = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    final var command = new EnrollPaymentInstrumentCommand(
        TEST_HASH_PAN.getValue(),
        SourceApp.ID_PAY.name(),
        true,
        null,
        null
    );
    final var result = service.handle(command);

    assertTrue(result);

    Mockito.verify(repository).save(argument.capture());
    assertEquals(TEST_HASH_PAN, argument.getValue().getHashPan());
    assertEquals(Collections.singleton(SourceApp.ID_PAY), argument.getValue().getEnabledApps());
  }

  @DisplayName("must be idempotent when enroll for same app")
  @Test
  public void mustBeIdempotentWhenEnableSameApp() {
    final var argument = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    final var commands = IntStream.range(0, 3).mapToObj(i -> new EnrollPaymentInstrumentCommand(
        TEST_HASH_PAN.getValue(),
        SourceApp.ID_PAY.name(),
        true,
        null,
        null
    )).collect(Collectors.toList());

    commands.forEach(command -> assertTrue(service.handle(command)));
    Mockito.verify(repository, Mockito.times(3)).save(argument.capture());

    assertEquals(TEST_HASH_PAN, argument.getValue().getHashPan());
    assertEquals(Collections.singleton(SourceApp.ID_PAY), argument.getValue().getEnabledApps());
  }

  @DisplayName("must disable payment instrument for a specific source app")
  @Test
  public void mustDisablePaymentInstrumentForSpecificApp() {
    final var fullEnrolledInstrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, new HashSet<>(Arrays.asList(SourceApp.values())), null, null);
    final var argument = ArgumentCaptor.forClass(EnrolledPaymentInstrument.class);
    final var command = new EnrollPaymentInstrumentCommand(
        TEST_HASH_PAN.getValue(),
        SourceApp.FA.name(),
        false,
        null,
        null
    );

    Mockito.when(repository.findByHashPan(Mockito.any())).thenReturn(Optional.of(fullEnrolledInstrument));

    assertTrue(service.handle(command));
    Mockito.verify(repository).save(argument.capture());

    assertEquals(TEST_HASH_PAN, argument.getValue().getHashPan());
    assertTrue(argument.getValue().getEnabledApps().size() > 0);
    assertTrue(argument.getValue().getEnabledApps().stream().noneMatch(i -> i == SourceApp.FA));
  }

  @DisplayName("must delete the payment instrument when enabled apps are empties")
  @Test
  public void mustDeleteWhenAppsAreEmpties() {
    final var fullEnrolledInstrument = EnrolledPaymentInstrument.create(TEST_HASH_PAN, new HashSet<>(Arrays.asList(SourceApp.values())), null, null);
    final var commands = Arrays.stream(SourceApp.values()).map(app -> new EnrollPaymentInstrumentCommand(
        TEST_HASH_PAN.getValue(),
        app.name(),
        false,
        null,
        null
    ));

    Mockito.when(repository.findByHashPan(Mockito.any())).thenReturn(Optional.of(fullEnrolledInstrument));

    commands.forEach(command -> service.handle(command));

    Mockito.verify(repository, Mockito.times(1)).delete(Mockito.any());
  }

  @DisplayName("must throw exception when command is invalid")
  @Test
  public void mustThrowExceptionWhenCommandIsInvalid() {
    final var invalidCommands = Arrays.asList(
        new EnrollPaymentInstrumentCommand(TEST_HASH_PAN.getValue(), "", true, null, null),
        new EnrollPaymentInstrumentCommand(TEST_HASH_PAN.getValue(), null, true, null, null),
        new EnrollPaymentInstrumentCommand("", SourceApp.ID_PAY.name(), true, null, null),
        new EnrollPaymentInstrumentCommand(null, SourceApp.ID_PAY.name(), true, null, null),
        new EnrollPaymentInstrumentCommand("", "", true, null, null),
        new EnrollPaymentInstrumentCommand(null, null, true, null, null)
    );

    assertTrue(invalidCommands.stream().noneMatch(command -> service.handle(command)));
  }
}
