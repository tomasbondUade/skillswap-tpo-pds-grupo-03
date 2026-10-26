package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.BookingData.BookingCommand;
import ar.edu.uade.skillswap.application.model.BookingData.BookingView;
import ar.edu.uade.skillswap.application.model.BookingData.CancelCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RejectCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RequestCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RescheduleCommand;
import java.util.List;

/** Inbound port for the booking lifecycle; each operation is a separate use case. */
public interface BookingUseCases {
    List<BookingView> listMyBookings();
    BookingView request(RequestCommand command);
    BookingView accept(BookingCommand command);
    BookingView reject(RejectCommand command);
    BookingView pay(BookingCommand command);
    BookingView cancel(CancelCommand command);
    BookingView proposeReschedule(RescheduleCommand command);
    BookingView acceptReschedule(BookingCommand command);
    BookingView start(BookingCommand command);
    BookingView complete(BookingCommand command);
}
