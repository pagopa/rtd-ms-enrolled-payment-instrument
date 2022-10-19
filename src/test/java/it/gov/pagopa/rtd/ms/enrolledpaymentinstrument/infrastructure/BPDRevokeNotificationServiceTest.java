package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@TestInstance(PER_CLASS)
@RestClientTest
class BPDRevokeNotificationServiceTest {

  @Autowired
  private MockRestServiceServer server;

  @Autowired
  private RestTemplateBuilder restTemplateBuilder;

  private BPDRevokeNotificationService bpdRevokeNotificationService;

  @BeforeAll
  void setup() {
    bpdRevokeNotificationService = new BPDRevokeNotificationService(restTemplateBuilder, false);
  }

  @AfterEach
  public void teardown() {
    server.reset();
  }

  @Test
  void whenBPDReturnsSuccessThenNotificationServiceReturnsTrue() {
    final var hashPan = TestUtils.generateRandomHashPan();
    server.expect(requestTo("/" + hashPan.getValue() + "?fiscalCode=CF"))
            .andRespond(withSuccess());

    assertThat(bpdRevokeNotificationService.notifyRevoke("CF", hashPan)).isTrue();
  }

  @Test
  void whenBPDReturnsFailureThenNotificationServiceReturnsFalse() {
    final var hashPan = TestUtils.generateRandomHashPan();
    server.expect(requestTo("/" + hashPan.getValue() + "?fiscalCode=CF"))
            .andRespond(withBadRequest());

    assertThat(bpdRevokeNotificationService.notifyRevoke("CF", hashPan)).isFalse();
  }

  @Test
  void whenBPDNotificationServiceIsFakeThenReturnsTrue() {
    final var fakeBpdNotificationService = new BPDRevokeNotificationService(restTemplateBuilder, true);
    final var hashPan = TestUtils.generateRandomHashPan();
    
    assertThat(fakeBpdNotificationService.notifyRevoke("CF", hashPan)).isTrue();
  }

}