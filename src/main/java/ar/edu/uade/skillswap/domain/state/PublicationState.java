package ar.edu.uade.skillswap.domain.state;

import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;

/** State protocol: each publication status owns its valid transitions. */
public interface PublicationState {
    PublicationStatus status();
    default PublicationState publish(Publication publication) { throw invalid("publicar"); }
    default PublicationState pause(Publication publication) { throw invalid("pausar"); }
    default PublicationState close(Publication publication) { throw invalid("cerrar"); }
    default PublicationState cancel(Publication publication, String reason) { throw invalid("cancelar"); }
    default boolean acceptsRequests() { return false; }
    private IllegalStateException invalid(String action) { return new IllegalStateException("No se puede " + action + " una publicación en estado " + status()); }
}
