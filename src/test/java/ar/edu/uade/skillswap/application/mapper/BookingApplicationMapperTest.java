package ar.edu.uade.skillswap.application.mapper;

import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingApplicationMapperTest {
    @Test
    void mapsRequestDetailsWithoutExposingTheDomainEntity() {
        Publication publication = Publication.draft("provider", "Java", "Descripción", "Tecnología",
                Modality.REMOTE, 60, 1, new SkillCoins(20), List.of("Java"));
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2);
        publication.addAvailability("provider", AvailabilitySlot.of(startsAt, startsAt.plusHours(1), 1));
        publication.publish("provider");

        Booking booking = Booking.request(publication, "buyer", "Necesito revisar un proyecto",
                "Preparar una entrevista técnica", publication.availability().get(0).id(), new SkillCoins(20));

        BookingView view = BookingApplicationMapper.toView(booking);

        assertEquals("Necesito revisar un proyecto", view.clientMessage());
        assertEquals("Preparar una entrevista técnica", view.objective());
        assertEquals(booking.slotId(), view.slotId());
    }
}
