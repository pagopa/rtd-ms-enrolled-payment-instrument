package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.infrastructure;

import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.HashPan;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.entities.SourceApp;
import it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.domain.services.InstrumentRevokeNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;

/**
 * This implementation allows to notify BPD microservice when a card is revoked (deleted from PM).
 */
@Slf4j
public class BPDRevokeNotificationService implements InstrumentRevokeNotificationService {

  private final RestTemplate restTemplate;
  private final boolean isFake;

  public static BPDRevokeNotificationService fake() {
    log.info("BPDRevokeNotificationService created as fake");
    return new BPDRevokeNotificationService(new RestTemplateBuilder(), true);
  }

  public static BPDRevokeNotificationService fromUrl(String url) {
    return new BPDRevokeNotificationService(new RestTemplateBuilder().rootUri(url), false);
  }

  private BPDRevokeNotificationService(RestTemplateBuilder builder, boolean isFake) {
    this.restTemplate = builder.build();
    this.isFake = isFake;
  }

  @Override
  public boolean notifyRevoke(Set<SourceApp> apps, String taxCode, HashPan hashPan) {
    if (isFake) {
      log.info("Fake BPD revoke card notification");
      return true;
    }

    try {
      final var url = UriComponentsBuilder.fromPath("/")
              .path("{hashPan}")
              .query("fiscalCode={taxCode}")
              .build(hashPan.getValue(), taxCode);
      return restTemplate.exchange(url.toString(), HttpMethod.DELETE, null, String.class)
              .getStatusCode().is2xxSuccessful();
    } catch (HttpClientErrorException.NotFound exception) {
      log.warn("BPD has return Not Found, the payment instrument doesn't exist in BPD. You can ignore this");
      return true;
    } catch (RestClientException exception) {
      log.error("Failed to notify BPD", exception);
      return false;
    }
  }
}
