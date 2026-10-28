package ar.edu.uade.skillswap.application.mapper;

import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.domain.model.Booking;

/** Converts Booking aggregates to application read models. */
public final class BookingApplicationMapper {
    private BookingApplicationMapper() {}

    public static BookingView toView(Booking booking) {
        return new BookingView(booking.id(), booking.publicationId(), booking.buyerUserId(),
                booking.providerUserId(), booking.objective(), booking.clientMessage(), booking.slotId(),
                booking.price().amount(), booking.refund().amount(), booking.status(),
                booking.createdAt(), booking.paidAt());
    }
}
