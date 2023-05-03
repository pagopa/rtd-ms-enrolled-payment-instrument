package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event;

import static org.assertj.core.api.Assertions.assertThat;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.EnrolledPaymentInstrumentService;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application.command.ExportCommand;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common.CloudEvent;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configs.KafkaTestConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.configurations.KafkaConfiguration;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.PaymentInstrumentExported;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("kafka-test")
@ExtendWith(SpringExtension.class)
@Import({
    TestChannelBinderConfiguration.class,
    ExportEventAdapter.class,
    KafkaTestConfiguration.class,
    KafkaConfiguration.class,
})
class ExportEventAdapterTest {

  private static final String INPUT_TOPIC = "rtd-split-by-pi";

  @Autowired
  private EnrolledPaymentInstrumentService paymentInstrumentService;

  @Autowired
  private InputDestination inputDestination;

  @AfterEach
  void teardown() {
    Mockito.reset(paymentInstrumentService);
  }

  @Test
  void whenExportEventThenExecuteValidExportCommand() throws JsonProcessingException {
    final var captor = ArgumentCaptor.forClass(ExportCommand.class);
    final var event = CloudEvent.<PaymentInstrumentExported>builder()
        .withType(PaymentInstrumentExported.TYPE)
        .withData(new PaymentInstrumentExported(TestUtils.generateRandomHashPanAsString()))
        .build();

    sendMessageAsJson(event, INPUT_TOPIC);

    Mockito.verify(paymentInstrumentService).handle(captor.capture());
    assertThat(captor.getValue().getHashPan()).isEqualTo(event.getData().getPaymentInstrumentId());
    assertThat(captor.getValue().getExportedAt()).isNotNull();
  }

  @Test
  void whenExportEventWithMissingMandatoryFieldsThenAdapterShouldNotCallService()
      throws JsonProcessingException {
    final var event = CloudEvent.<PaymentInstrumentExported>builder()
        .withType(TokenManagerCardChanged.TYPE)
        .withData(new PaymentInstrumentExported(null))
        .build();

    sendMessageAsJson(event, INPUT_TOPIC);
    Mockito.verify(paymentInstrumentService, Mockito.times(0))
        .handle(Mockito.any(ExportCommand.class));
  }

  private <T> void sendMessageAsJson(T payload, String inputName) throws JsonProcessingException {
    inputDestination.send(
        MessageBuilder.withPayload(new ObjectMapper().writeValueAsString(payload)).build(),
        inputName
    );
  }
}