package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.error.*;
import ar.edu.uade.skillswap.application.model.BookingData.*;
import ar.edu.uade.skillswap.application.port.in.BookingUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.model.Models;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Controller delgado: valida identidad y entrada, delega las reglas a Application/Domain. */
public final class ContratacionUiController implements UiControllers.ContratacionController {
    private final BookingUseCases bookings;
    private final CurrentUserProvider identity;
    private final ControllerExecutor operations;
    private final Function<BookingView, Models.Contratacion> toUi;

    public ContratacionUiController(BookingUseCases bookings, CurrentUserProvider identity,
            ControllerExecutor operations, Function<BookingView, Models.Contratacion> toUi) {
        this.bookings = Objects.requireNonNull(bookings);
        this.identity = Objects.requireNonNull(identity);
        this.operations = Objects.requireNonNull(operations);
        this.toUi = Objects.requireNonNull(toUi);
    }

    @Override public List<Models.Contratacion> listarContrataciones(String userId) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return bookings.listMyBookings().stream().map(toUi).toList();
        });
    }

    @Override public Models.Contratacion solicitar(String userId, String publicacionId,
            String mensajeCliente, String objetivo, String franjaId) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            ControllerUser.required(publicacionId, "Falta la publicación");
            ControllerUser.required(objetivo, "Falta el objetivo");
            ControllerUser.required(franjaId, "Falta la franja");
            return toUi.apply(bookings.request(new RequestCommand(publicacionId.trim(),
                    mensajeCliente, objetivo.trim(), franjaId.trim())));
        });
    }

    @Override public Models.Contratacion aceptar(String userId, String id) {
        return change(userId, id, bookings::accept);
    }
    @Override public Models.Contratacion rechazar(String userId, String id, String motivo) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            String bookingId = requiredId(id);
            ControllerUser.required(motivo, "Falta el motivo del rechazo");
            return toUi.apply(bookings.reject(new RejectCommand(bookingId, motivo.trim())));
        });
    }
    @Override public Models.Contratacion pagar(String userId, String id) {
        return change(userId, id, bookings::pay);
    }
    @Override public Models.Contratacion cancelar(String userId, String id) {
        return cancelar(userId, id, "Cancelada desde la interfaz");
    }
    @Override public Models.Contratacion cancelar(String userId, String id, String motivo) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            String bookingId = requiredId(id);
            ControllerUser.required(motivo, "Falta el motivo de la cancelación");
            return toUi.apply(bookings.cancel(new CancelCommand(bookingId, motivo.trim())));
        });
    }
    @Override public Models.Contratacion proponerReprogramacion(String userId, String id, String franjaId) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            String bookingId = requiredId(id);
            ControllerUser.required(franjaId, "Falta la nueva franja");
            return toUi.apply(bookings.proposeReschedule(new RescheduleCommand(bookingId, franjaId.trim())));
        });
    }
    @Override public Models.Contratacion aceptarReprogramacion(String userId, String id) {
        return change(userId, id, bookings::acceptReschedule);
    }
    @Override public Models.Contratacion iniciar(String userId, String id) {
        return change(userId, id, bookings::start);
    }
    @Override public Models.Contratacion completar(String userId, String id) {
        return change(userId, id, bookings::complete);
    }

    /** Compatibilidad con el botón Avanzar de Swing. */
    @Override public Models.Contratacion avanzar(String userId, String id) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            String bookingId = requiredId(id);
            BookingView current = findMine(bookingId);
            BookingCommand command = new BookingCommand(bookingId);
            BookingView updated = switch (current.status()) {
                case PENDING -> bookings.accept(command);
                case ACCEPTED -> bookings.pay(command);
                case CONFIRMED -> bookings.start(command);
                case IN_PROGRESS -> bookings.complete(command);
                case COMPLETED, REJECTED, CANCELLED -> throw new ApplicationException(
                        ApplicationErrorCode.INVALID_STATE, "La contratación no admite más avances");
            };
            return toUi.apply(updated);
        });
    }

    @Override public boolean puedeAvanzar(String userId, String id) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            BookingView booking = findMine(requiredId(id));
            String actor = userId.trim();
            return switch (booking.status()) {
                case PENDING -> booking.providerUserId().equals(actor);
                case ACCEPTED -> booking.buyerUserId().equals(actor);
                case CONFIRMED, IN_PROGRESS -> booking.buyerUserId().equals(actor)
                        || booking.providerUserId().equals(actor);
                case COMPLETED, REJECTED, CANCELLED -> false;
            };
        });
    }

    private Models.Contratacion change(String userId, String id,
            Function<BookingCommand, BookingView> action) {
        return operations.execute(() -> {
            ControllerUser.requireActive(identity, userId);
            return toUi.apply(action.apply(new BookingCommand(requiredId(id))));
        });
    }

    private BookingView findMine(String id) {
        return bookings.listMyBookings().stream()
                .filter(item -> item.id().equals(id)).findFirst()
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.NOT_FOUND,
                        "Contratación inexistente o no disponible"));
    }

    private static String requiredId(String id) {
        ControllerUser.required(id, "Falta el identificador de contratación");
        return id.trim();
    }
}
