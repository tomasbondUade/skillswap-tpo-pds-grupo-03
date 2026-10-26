package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.application.event.DomainEvent;

/** Publishes application-observed domain events without binding use cases to Observer or a broker. */
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
