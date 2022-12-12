package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure.tkm;

import io.vavr.control.Try;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.InstrumentTokenInfo;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentTokenFinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TkmClient implements InstrumentTokenFinder {

  private static final String AUTHORIZATION_HEADER = "Ocp-Apim-Subscription-Key";
  private static final String IF_MATCH_HEADER = "If-Match";

  private final RestTemplate restTemplate;
  private final String apiKey;

  public static TkmClient fromUrl(String url, String apiKey) {
    return new TkmClient(new RestTemplateBuilder().rootUri(url), apiKey);
  }

  private TkmClient(RestTemplateBuilder restTemplate, String apiKey) {
    this.restTemplate = restTemplate.build();
    this.apiKey = apiKey;
  }

  @Override
  public Try<InstrumentTokenInfo> findInstrumentInfo(HashPan hashPan) {
    final var url = UriComponentsBuilder.fromPath("/").build();
    final var headers = new HttpHeaders();
    if (Objects.nonNull(apiKey) && !apiKey.isBlank()) {
      headers.set(AUTHORIZATION_HEADER, apiKey);
    }
    headers.set(IF_MATCH_HEADER, hashPan.getValue());

    return Try.of(() -> restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    TkmInstrumentResponse.class
            ))
            .map(HttpEntity::getBody)
            .flatMap(this::toDomain);
  }

  private Try<InstrumentTokenInfo> toDomain(TkmInstrumentResponse response) {
    return Try.of(() -> new InstrumentTokenInfo(
            HashPan.create(response.hashPan),
            response.getPar(),
            Optional.ofNullable(response.getHashTokens()).orElse(List.of()).stream().map(HashPan::create).collect(Collectors.toList())
    ));
  }
}
