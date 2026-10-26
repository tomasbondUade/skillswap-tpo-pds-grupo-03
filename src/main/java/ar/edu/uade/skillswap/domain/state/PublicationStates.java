package ar.edu.uade.skillswap.domain.state;

import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;

/** Concrete State objects for the publication lifecycle. */
public final class PublicationStates {
    private PublicationStates() {}
    public static PublicationState draft() { return Draft.INSTANCE; }
    public static PublicationState published() { return Published.INSTANCE; }
    public static PublicationState paused() { return Paused.INSTANCE; }
    public static PublicationState closed() { return Closed.INSTANCE; }
    public static PublicationState cancelled() { return Cancelled.INSTANCE; }

    private enum Draft implements PublicationState {
        INSTANCE;
        public PublicationStatus status() { return PublicationStatus.DRAFT; }
        @Override public PublicationState publish(Publication p) { p.validateBeforePublishing(); return published(); }
        @Override public PublicationState cancel(Publication p, String reason) { Publication.requireCancellationReason(reason); return cancelled(); }
    }
    private enum Published implements PublicationState {
        INSTANCE;
        public PublicationStatus status() { return PublicationStatus.PUBLISHED; }
        @Override public PublicationState pause(Publication p) { return paused(); }
        @Override public PublicationState close(Publication p) { return closed(); }
        @Override public PublicationState cancel(Publication p, String reason) { Publication.requireCancellationReason(reason); return cancelled(); }
        @Override public boolean acceptsRequests() { return true; }
    }
    private enum Paused implements PublicationState {
        INSTANCE;
        public PublicationStatus status() { return PublicationStatus.PAUSED; }
        @Override public PublicationState publish(Publication p) { p.validateBeforePublishing(); return published(); }
        @Override public PublicationState close(Publication p) { return closed(); }
        @Override public PublicationState cancel(Publication p, String reason) { Publication.requireCancellationReason(reason); return cancelled(); }
    }
    private enum Closed implements PublicationState { INSTANCE; public PublicationStatus status() { return PublicationStatus.CLOSED; } }
    private enum Cancelled implements PublicationState { INSTANCE; public PublicationStatus status() { return PublicationStatus.CANCELLED; } }
}
