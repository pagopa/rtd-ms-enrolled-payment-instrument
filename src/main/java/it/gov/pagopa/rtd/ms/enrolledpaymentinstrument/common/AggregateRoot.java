package it.gov.pagopa.rtd.ms.enrolledpaymentinstrument.common;


import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstraction over most important concept of DDD. Also allow to use Domain Events to de-coupling business logic
 * with application logic (like publishing events).
 * An aggregate root must allow to achieve a transactional boundary execution, where a specific domain logic
 * update data in a consistent way.
 */
public abstract class AggregateRoot {

  @Transient
  private final List<DomainEvent> domainEvents;

  protected AggregateRoot() {
    this(new ArrayList<>());
  }

  protected AggregateRoot(List<DomainEvent> domainEvents) {
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