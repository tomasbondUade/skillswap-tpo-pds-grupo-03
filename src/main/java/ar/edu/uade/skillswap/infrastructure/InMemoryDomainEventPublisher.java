package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.event.DomainEvent;
import ar.edu.uade.skillswap.application.port.out.DomainEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class InMemoryDomainEventPublisher implements DomainEventPublisher {
    private final Map<String, List<Consumer<DomainEvent>>> observers = new ConcurrentHashMap<>();

    public void subscribe(String eventName, Consumer<DomainEvent> observer) {
        observers.computeIfAbsent(eventName, ignored -> new CopyOnWriteArrayList<>()).add(observer);
    }

    @Override
    public void publish(DomainEvent event) {
        RuntimeException failure = null;
        for (Consumer<DomainEvent> observer : observers.getOrDefault(event.name(), List.of())) {
            try {
                observer.accept(event);
            } catch (RuntimeException error) {
                if (failure == null) {
                    failure = new IllegalStateException("Falló uno o más observadores del evento " + event.name());
                }
                failure.addSuppressed(error);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}