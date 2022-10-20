package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common;

import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot {

  @Transient
  private final List<DomainEvent> domainEvents;

  public AggregateRoot() {
    this(new ArrayList<>());
  }

  AggregateRoot(List<DomainEvent> domainEvents) {
    this.domainEvents = domainEvents;
  }

  protected void registerEvent(DomainEvent event) {
    domainEvents.add(event);
  }

  public List<DomainEvent> domainEvents() {
    return Collections.unmodifiableList(domainEvents);
  }

  public void clearDomainEvents() {
    domainEvents.clear();
  }

}
