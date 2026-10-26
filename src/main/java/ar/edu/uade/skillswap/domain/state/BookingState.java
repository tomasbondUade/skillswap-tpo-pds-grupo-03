package ar.edu.uade.skillswap.domain.state;

import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.BookingStatus;

/** State protocol for a mentoring request and its lifecycle. */
public interface BookingState {
    BookingStatus status();
    default BookingState accept(Booking b) { throw invalid("aceptar"); }
    default BookingState reject(Booking b) { throw invalid("rechazar"); }
    default BookingState pay(Booking b) { throw invalid("pagar"); }
    default BookingState start(Booking b) { throw invalid("iniciar"); }
    default BookingState complete(Booking b) { throw invalid("completar"); }
    default BookingState cancel(Booking b) { throw invalid("cancelar"); }
    private IllegalStateException invalid(String action) { return new IllegalStateException("No se puede " + action + " una contratación en estado " + status()); }
}
