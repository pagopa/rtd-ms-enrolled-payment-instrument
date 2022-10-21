package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import org.junit.Rule;
import org.junit.jupiter.api.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
class BPDRevokeNotificationServiceTest {

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8089);

  private BPDRevokeNotificationService bpdRevokeNotificationService;

  @BeforeAll
  void setup() {
    wireMockRule.start();
    configureFor("localhost", wireMockRule.port());
    bpdRevokeNotificationService = BPDRevokeNotificationService.fromUrl("http://localhost:" + wireMockRule.port());
  }

  @AfterEach
  void clean() {
    WireMock.reset();
  }

  @AfterAll
  public void teardown() {
    wireMockRule.stop();
  }

  @Test
  void whenBPDReturnsSuccessThenNotificationServiceReturnsTrue() {
    final var hashPan = TestUtils.generateRandomHashPan();
    stubFor(delete("/" + hashPan.getValue() + "?fiscalCode=CF")
            .willReturn(ok()));

    assertThat(bpdRevokeNotificationService.notifyRevoke("CF", hashPan)).isTrue();
  }

  @Test
  void whenRevokeNonExistingBPDCardThenReturnsTrue() {
    final var hashPan = TestUtils.generateRandomHashPan();
    stubFor(delete("/" + hashPan.getValue() + "?fiscalCode=CF")
            .willReturn(notFound()));
    assertThat(bpdRevokeNotificationService.notifyRevoke("CF", hashPan)).isTrue();
  }

  @Test
  void whenBPDReturnsFailureThenNotificationServiceReturnsFalse() {
    final var hashPan = TestUtils.generateRandomHashPan();
    stubFor(delete("/" + hashPan.getValue() + "?fiscalCode=CF")
            .willReturn(serverError()));

    assertThat(bpdRevokeNotificationService.notifyRevoke("CF", hashPan)).isFalse();
  }

  @Test
  void whenBPDNotificationServiceIsFakeThenReturnsTrue() {
    final var fakeBpdNotificationService = BPDRevokeNotificationService.fake();
    final var hashPan = TestUtils.generateRandomHashPan();

    assertThat(fakeBpdNotificationService.notifyRevoke("CF", hashPan)).isTrue();
  }
}