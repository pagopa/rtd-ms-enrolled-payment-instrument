package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.kafka.splitter.TokenManagerCardEventPublisher;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.ports.event.dto.TokenManagerCardChanged;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentManagerBackwardControllerImpl.class)
class PaymentManagerBackwardControllerImplTest {

  private static final String BASE_URI = "http://localhost:8080/pm-backward/";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TokenManagerCardEventPublisher cardEventPublisher;

  private final ObjectMapper mapper = new ObjectMapper();

  @AfterEach
  void teardown() {
    Mockito.reset(cardEventPublisher);
  }

  @Test
  void whenRevokeCardThenReturnsOkAndSendAnEvent() throws Exception {
    Mockito.doReturn(true).when(cardEventPublisher).sendTokenManagerCardChanged(Mockito.any());
    mockMvc.perform(
            MockMvcRequestBuilders.delete(BASE_URI + "hashPan?taxCode=taxCode")
    ).andExpectAll(status().isOk());

    Mockito.verify(cardEventPublisher, Mockito.times(1)).sendTokenManagerCardChanged(Mockito.any());
  }

  @Test
  void whenRevokeCardThenARevokeEventIsCreated() throws Exception {
    final var captor = ArgumentCaptor.forClass(TokenManagerCardChanged.class);
    Mockito.doReturn(true).when(cardEventPublisher).sendTokenManagerCardChanged(Mockito.any());
    mockMvc.perform(
            MockMvcRequestBuilders.delete(BASE_URI + "hashPan?taxCode=taxCode")
    ).andExpectAll(status().isOk());

    Mockito.verify(cardEventPublisher).sendTokenManagerCardChanged(captor.capture());

    assertThat(captor.getValue())
            .matches(it -> "hashPan".equals(it.getHashPan()))
            .matches(it -> "taxCode".equals(it.getTaxCode()))
            .matches(it -> it.getHashTokens().isEmpty())
            .matches(it -> Objects.isNull(it.getPar()));
  }

  @Test
  void whenRevokeCardFiscalCodeMissingThenReturnBadRequest() throws Exception {
    Mockito.doReturn(true).when(cardEventPublisher).sendTokenManagerCardChanged(Mockito.any());
    mockMvc.perform(
            MockMvcRequestBuilders.delete(BASE_URI + "hashPan")
    ).andExpectAll(status().isBadRequest());
  }

  @Test
  void whenFailToPublishEventThenReturnInternalServerError() throws Exception {
    Mockito.doReturn(false).when(cardEventPublisher).sendTokenManagerCardChanged(Mockito.any());
    mockMvc.perform(
            MockMvcRequestBuilders.delete(BASE_URI + "hashPan?taxCode=taxCode")
    ).andExpectAll(status().isInternalServerError());
  }
}