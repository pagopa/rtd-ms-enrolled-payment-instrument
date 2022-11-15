package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public final class DomainEventPublisher {

  private final ApplicationEventPublisher eventPublisher;

  public DomainEventPublisher(ApplicationEventPublisher eventPublisher){
    this.eventPublisher = eventPublisher;
  }

  public void handle(AggregateRoot aggregateRoot) {
    aggregateRoot.domainEvents().forEach(eventPublisher::publishEvent);
    aggregateRoot.clearDomainEvents();
  }
}
