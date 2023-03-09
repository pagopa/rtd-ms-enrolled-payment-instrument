package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.application;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StreamHealthIndicator extends AbstractHealthIndicator {
  private final ApplicationContext applicationContext;

  public StreamHealthIndicator(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    final var subscriptionCounts = applicationContext.getBeansOfType(DirectWithAttributesChannel.class)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSubscriberCount()));
    if(subscriptionCounts.values().stream().anyMatch(subscriptions -> subscriptions == 0)){
      builder.down();
    } else {
      builder.up();
    }
    builder.withDetails(subscriptionCounts);
  }
}
