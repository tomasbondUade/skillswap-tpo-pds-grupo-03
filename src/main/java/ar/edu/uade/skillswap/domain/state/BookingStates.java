package ar.edu.uade.skillswap.domain.state;

import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.BookingStatus;

/** Concrete State objects; terminal states intentionally expose no transitions. */
public final class BookingStates {
    private BookingStates() {}
    public static BookingState pending() { return Pending.INSTANCE; }
    public static BookingState accepted() { return Accepted.INSTANCE; }
    public static BookingState confirmed() { return Confirmed.INSTANCE; }
    public static BookingState inProgress() { return InProgress.INSTANCE; }
    public static BookingState completed() { return Completed.INSTANCE; }
    public static BookingState rejected() { return Rejected.INSTANCE; }
    public static BookingState cancelled() { return Cancelled.INSTANCE; }
    private enum Pending implements BookingState {
        INSTANCE; public BookingStatus status() { return BookingStatus.PENDING; }
        @Override public BookingState accept(Booking b) { return accepted(); }
        @Override public BookingState reject(Booking b) { return rejected(); }
        @Override public BookingState cancel(Booking b) { return cancelled(); }
    }
    private enum Accepted implements BookingState {
        INSTANCE; public BookingStatus status() { return BookingStatus.ACCEPTED; }
        @Override public BookingState pay(Booking b) { return confirmed(); }
        @Override public BookingState cancel(Booking b) { return cancelled(); }
    }
    private enum Confirmed implements BookingState {
        INSTANCE; public BookingStatus status() { return BookingStatus.CONFIRMED; }
        @Override public BookingState start(Booking b) { return inProgress(); }
        @Override public BookingState cancel(Booking b) { return cancelled(); }
    }
    private enum InProgress implements BookingState {
        INSTANCE; public BookingStatus status() { return BookingStatus.IN_PROGRESS; }
        @Override public BookingState complete(Booking b) { return completed(); }
    }
    private enum Completed implements BookingState { INSTANCE; public BookingStatus status() { return BookingStatus.COMPLETED; } }
    private enum Rejected implements BookingState { INSTANCE; public BookingStatus status() { return BookingStatus.REJECTED; } }
    private enum Cancelled implements BookingState { INSTANCE; public BookingStatus status() { return BookingStatus.CANCELLED; } }
}
