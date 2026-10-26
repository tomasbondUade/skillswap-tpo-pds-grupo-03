package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.domain.model.Booking;
import java.util.List;
import java.util.Optional;

/** Persistence contract; lock-aware reads are required for concurrent lifecycle changes. */
public interface BookingRepository {
    Optional<Booking> findById(String bookingId);
    Optional<Booking> findByIdForUpdate(String bookingId);
    List<Booking> findByParticipantUserId(String userId);
    Booking save(Booking booking);
}
