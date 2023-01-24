package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.tkm;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.TestUtils;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentTokenFinder;
import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TkmTokenFinderTest {

  private static final String PATH_CARD_STUB = "/cards";
  private static final String TEST_API_KEY = "ApiKey";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(8089);

  private InstrumentTokenFinder instrumentTokenFinder;

  @BeforeAll
  void setup() {
    wireMockRule.start();
    configureFor("localhost", wireMockRule.port());
    instrumentTokenFinder = TkmTokenFinder.fromUrl(
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
    final var stubTokenInfo = new TkmInstrumentResponse(
            hashPan.getValue(),
            "par",
            IntStream.range(1, 3).mapToObj(i -> TestUtils.generateRandomHashPanAsString()).collect(Collectors.toList())
    );
    stubFor(get(PATH_CARD_STUB).willReturn(ResponseDefinitionBuilder.okForJson(stubTokenInfo)));

    assertThat(instrumentTokenFinder.findInstrumentInfo(hashPan).get())
            .matches(it -> it.getPar().get().equals(stubTokenInfo.getPar()))
            .matches(it -> it.getHashPan().getValue().equals(stubTokenInfo.getHashPan()))
            .satisfies(it -> assertThat(it.getHashTokens()).hasSameElementsAs(stubTokenInfo.getHashTokens().stream().map(HashPan::create).collect(Collectors.toList())));

    verify(exactly(1), getRequestedFor(urlEqualTo(PATH_CARD_STUB))
            .withHeader("hpan", equalTo(hashPan.getValue()))
            .withHeader("Ocp-Apim-Subscription-Key", equalTo(TEST_API_KEY))
    );
  }

  @Test
  void whenNotFoundInstrumentTokenInfoThenReturnsFailure() {
    final var hashPan = TestUtils.generateRandomHashPan();
    stubFor(get(PATH_CARD_STUB).willReturn(notFound()));

    assertThat(instrumentTokenFinder.findInstrumentInfo(hashPan).getCause())
            .matches(it -> it.getMessage().contains("Not Found"));
  }

  @Test
  void whenTkmReturnErrorThenReturnsFailure() {
    stubFor(get(PATH_CARD_STUB).willReturn(serverError()));

    assertThat(instrumentTokenFinder.findInstrumentInfo(TestUtils.generateRandomHashPan()).getCause())
            .matches(it -> it.getMessage().contains("Server Error"));
  }

  @Nested
  class FakeTokenFinder {

    private InstrumentTokenFinder tokenFinder;

    @BeforeEach
    void setup() {
      tokenFinder = InstrumentTokenFinder.fake(LoggerFactory.getLogger("test"));
    }

    @Test
    void whenFindInstrumentThenReturnsEmptyParAndTokens() {
      final var hashPan = TestUtils.generateRandomHashPan();
      assertThat(tokenFinder.findInstrumentInfo(hashPan))
              .first()
              .matches(it -> it.getPar().equals(Optional.of("")))
              .matches(it -> it.getHashTokens().isEmpty());
    }
  }
}