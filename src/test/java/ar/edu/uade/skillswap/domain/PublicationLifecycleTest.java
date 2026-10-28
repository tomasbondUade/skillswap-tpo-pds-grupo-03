package ar.edu.uade.skillswap.domain;

import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.model.StatusHistoryEntry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PublicationLifecycleTest {
    private static final String AUTHOR = "author";

    @Test
    void publishingRequiresCompleteDetailsAndAvailabilityAndAcceptsZeroPrice() {
        Publication noTitle = draft(" ", "Descripción", "Tecnología", true);
        Publication noDescription = draft("Java", " ", "Tecnología", true);
        Publication noCategory = draft("Java", "Descripción", " ", true);
        Publication noAvailability = draft("Java", "Descripción", "Tecnología", false);

        assertPublishRejectedWithoutMutation(noTitle);
        assertPublishRejectedWithoutMutation(noDescription);
        assertPublishRejectedWithoutMutation(noCategory);
        assertPublishRejectedWithoutMutation(noAvailability);
        assertThrows(IllegalArgumentException.class, () -> new SkillCoins(-1));

        Publication freePublication = readyDraft(new SkillCoins(0));
        assertDoesNotThrow(() -> freePublication.publish(AUTHOR));
        assertEquals(PublicationStatus.PUBLISHED, freePublication.status());
        assertEquals(0, freePublication.basePrice().amount());
    }

    @Test
    void draftCanBePublishedOrCancelledAndRejectsOtherTransitions() {
        Publication publishing = readyDraft();
        publishing.publish(AUTHOR);
        assertEquals(PublicationStatus.PUBLISHED, publishing.status());
        assertHistoryTransition(publishing, 1, PublicationStatus.DRAFT, PublicationStatus.PUBLISHED);

        Publication cancelling = readyDraft();
        cancelling.cancel(AUTHOR, "Retirada por el autor");
        assertEquals(PublicationStatus.CANCELLED, cancelling.status());
        assertEquals("Retirada por el autor", cancelling.history().get(1).reason());

        Publication draft = readyDraft();
        List<StatusHistoryEntry> originalHistory = draft.history();
        assertThrows(IllegalStateException.class, () -> draft.pause(AUTHOR));
        assertThrows(IllegalStateException.class, () -> draft.close(AUTHOR));
        assertThrows(IllegalArgumentException.class, () -> draft.cancel(AUTHOR, " "));
        assertThrows(SecurityException.class, () -> draft.publish("other-user"));
        assertEquals(PublicationStatus.DRAFT, draft.status());
        assertEquals(originalHistory, draft.history());
    }

    @Test
    void publishedCanPauseCloseOrCancelButCannotBePublishedAgain() {
        Publication pausing = published();
        pausing.pause(AUTHOR);
        assertEquals(PublicationStatus.PAUSED, pausing.status());

        Publication closing = published();
        closing.close(AUTHOR);
        assertEquals(PublicationStatus.CLOSED, closing.status());

        Publication cancelling = published();
        cancelling.cancel(AUTHOR, "Contenido desactualizado");
        assertEquals(PublicationStatus.CANCELLED, cancelling.status());

        Publication publication = published();
        List<StatusHistoryEntry> originalHistory = publication.history();
        assertThrows(IllegalStateException.class, () -> publication.publish(AUTHOR));
        assertEquals(PublicationStatus.PUBLISHED, publication.status());
        assertEquals(originalHistory, publication.history());
    }

    @Test
    void pausedCanBeRepublishedClosedOrCancelledButCannotBePausedAgain() {
        Publication republishing = paused();
        republishing.publish(AUTHOR);
        assertEquals(PublicationStatus.PUBLISHED, republishing.status());

        Publication closing = paused();
        closing.close(AUTHOR);
        assertEquals(PublicationStatus.CLOSED, closing.status());

        Publication cancelling = paused();
        cancelling.cancel(AUTHOR, "Pausa definitiva");
        assertEquals(PublicationStatus.CANCELLED, cancelling.status());

        Publication publication = paused();
        List<StatusHistoryEntry> originalHistory = publication.history();
        assertThrows(IllegalStateException.class, () -> publication.pause(AUTHOR));
        assertEquals(PublicationStatus.PAUSED, publication.status());
        assertEquals(originalHistory, publication.history());
    }

    @Test
    void closedAndCancelledAreTerminalAndDoNotAcceptRequests() {
        for (Publication publication : List.of(closed(), cancelled())) {
            List<StatusHistoryEntry> originalHistory = publication.history();
            String slotId = publication.availability().get(0).id();

            assertThrows(IllegalStateException.class, () -> publication.publish(AUTHOR));
            assertThrows(IllegalStateException.class, () -> publication.pause(AUTHOR));
            assertThrows(IllegalStateException.class, () -> publication.close(AUTHOR));
            assertThrows(IllegalStateException.class, () -> publication.cancel(AUTHOR, "Otro motivo"));
            assertThrows(IllegalStateException.class, () -> publication.recordBookingRequest("buyer", slotId));
            assertThrows(IllegalStateException.class, () -> publication.reserve(slotId));

            assertEquals(originalHistory, publication.history());
            assertEquals(0, publication.requestCount());
        }
    }

    @Test
    void onlyDraftCanBeEditedAndOnlyPublishedPublicationsAcceptRequests() {
        Publication draft = readyDraft();
        draft.edit(AUTHOR, "Java avanzado", "Nueva descripción", "Tecnología", Modality.REMOTE,
                60, 2, Set.of("Java", "Backend"));
        assertEquals("Java avanzado", draft.title());

        Publication publication = published();
        String slotId = publication.availability().get(0).id();
        publication.recordBookingRequest("buyer", slotId);
        assertEquals(1, publication.requestCount());
        assertEquals(0, publication.availability().get(0).reserved(),
                "Registrar una solicitud pendiente no reserva el cupo");
        assertThrows(IllegalStateException.class, () -> publication.edit(AUTHOR,
                "Otro título", "Descripción", "Tecnología", Modality.REMOTE, 60, 2, Set.of("Java")));

        for (Publication unavailable : List.of(paused(), closed(), cancelled())) {
            String unavailableSlotId = unavailable.availability().get(0).id();
            assertThrows(IllegalStateException.class,
                    () -> unavailable.recordBookingRequest("buyer", unavailableSlotId));
            assertThrows(IllegalStateException.class, () -> unavailable.edit(AUTHOR,
                    "Otro título", "Descripción", "Tecnología", Modality.REMOTE, 60, 2, Set.of("Java")));
            assertEquals(0, unavailable.requestCount());
        }

        assertThrows(IllegalStateException.class,
                () -> draft.recordBookingRequest("buyer", draft.availability().get(0).id()));
        assertEquals(0, draft.requestCount());
    }

    @Test
    void availabilityMustFitPublicationDurationCapacityAndSchedule() {
        Publication publication = draft("Java", "Descripción", "Tecnología", false);
        LocalDateTime start = LocalDateTime.parse("2026-11-01T10:00:00");

        assertThrows(IllegalArgumentException.class, () -> publication.addAvailability(AUTHOR,
                AvailabilitySlot.of(start, start.plusMinutes(30), 1)));
        assertThrows(IllegalArgumentException.class, () -> publication.addAvailability(AUTHOR,
                AvailabilitySlot.of(start, start.plusHours(1), 3)));

        publication.addAvailability(AUTHOR, AvailabilitySlot.of(start, start.plusHours(1), 2));
        assertThrows(IllegalArgumentException.class, () -> publication.addAvailability(AUTHOR,
                AvailabilitySlot.of(start.plusMinutes(30), start.plusHours(2), 1)));
        assertEquals(1, publication.availability().size(), "Una franja inválida no debe modificar la disponibilidad");
    }

    private static Publication draft(String title, String description, String category, boolean withAvailability) {
        Publication publication = Publication.draft(AUTHOR, title, description, category,
                Modality.REMOTE, 60, 2, new SkillCoins(10), Set.of("Java"));
        if (withAvailability) {
            publication.addAvailability(AUTHOR, AvailabilitySlot.of(
                    LocalDateTime.parse("2026-11-01T10:00:00"),
                    LocalDateTime.parse("2026-11-01T11:00:00"), 2));
        }
        return publication;
    }

    private static Publication readyDraft() {
        return readyDraft(new SkillCoins(10));
    }

    private static Publication readyDraft(SkillCoins price) {
        Publication publication = Publication.draft(AUTHOR, "Java", "Descripción", "Tecnología",
                Modality.REMOTE, 60, 2, price, Set.of("Java"));
        publication.addAvailability(AUTHOR, AvailabilitySlot.of(
                LocalDateTime.parse("2026-11-01T10:00:00"),
                LocalDateTime.parse("2026-11-01T11:00:00"), 2));
        return publication;
    }

    private static Publication published() {
        Publication publication = readyDraft();
        publication.publish(AUTHOR);
        return publication;
    }

    private static Publication paused() {
        Publication publication = published();
        publication.pause(AUTHOR);
        return publication;
    }

    private static Publication closed() {
        Publication publication = published();
        publication.close(AUTHOR);
        return publication;
    }

    private static Publication cancelled() {
        Publication publication = published();
        publication.cancel(AUTHOR, "Cancelada para la prueba");
        return publication;
    }

    private static void assertPublishRejectedWithoutMutation(Publication publication) {
        List<StatusHistoryEntry> originalHistory = publication.history();
        assertThrows(IllegalStateException.class, () -> publication.publish(AUTHOR));
        assertEquals(PublicationStatus.DRAFT, publication.status());
        assertEquals(originalHistory, publication.history());
    }

    private static void assertHistoryTransition(Publication publication, int index,
                                               PublicationStatus from, PublicationStatus to) {
        StatusHistoryEntry transition = publication.history().get(index);
        assertEquals(from.name(), transition.from());
        assertEquals(to.name(), transition.to());
    }
}
