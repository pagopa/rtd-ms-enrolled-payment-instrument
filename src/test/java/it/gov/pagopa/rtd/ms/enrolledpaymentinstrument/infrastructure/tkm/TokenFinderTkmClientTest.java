package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.tkm;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.InstrumentTokenInfo;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentTokenFinder;
import org.junit.Rule;
import org.junit.jupiter.api.*;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenFinderTkmClientTest {

  private static final String PATH_CARD_STUB = "/cards";
  private static final String TEST_API_KEY = "ApiKey";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8089);

  private InstrumentTokenFinder instrumentTokenFinder;

  @BeforeAll
  void setup() {
    wireMockRule.start();
    configureFor("localhost", wireMockRule.port());
    instrumentTokenFinder = TkmClient.fromUrl(
            "http://localhost:" + wireMockRule.port(),
            TEST_API_KEY
    );
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
  void whenFoundHashPanThenReturnsInstrumentTokenInfo() {
    final var hashPan = TestUtils.generateRandomHashPan();
    final var stubTokenInfo = new InstrumentTokenInfo(
            hashPan,
            "par",
            IntStream.range(1, 3).mapToObj(i -> TestUtils.generateRandomHashPan()).collect(Collectors.toList())
    );
    stubFor(get(PATH_CARD_STUB).willReturn(ResponseDefinitionBuilder.okForJson(stubTokenInfo)));

    assertThat(instrumentTokenFinder.findInstrumentInfo(hashPan))
            .allMatch(it -> it.equals(stubTokenInfo));

    verify(exactly(1), getRequestedFor(urlEqualTo(PATH_CARD_STUB))
            .withHeader("hpan", equalTo(hashPan.getValue()))
            .withHeader("Ocp-Apim-Subscription-Key", equalTo(TEST_API_KEY))
    );
  }

  @Test
  void whenNotFoundInstrumentTokenInfoThenReturnsFailure() {
    final var hashPan = TestUtils.generateRandomHashPan();
    stubFor(get(PATH_CARD_STUB).willReturn(notFound()));

    assertThat(instrumentTokenFinder.findInstrumentInfo(hashPan).failed())
            .allMatch(it -> it.getMessage().contains("Not Found"));
  }

  @Test
  void whenTkmReturnErrorThenReturnsFailure() {
    stubFor(get(PATH_CARD_STUB).willReturn(serverError()));

    assertThat(instrumentTokenFinder.findInstrumentInfo(TestUtils.generateRandomHashPan()).failed())
            .allMatch(it -> it.getMessage().contains("Server Error"));
  }
}