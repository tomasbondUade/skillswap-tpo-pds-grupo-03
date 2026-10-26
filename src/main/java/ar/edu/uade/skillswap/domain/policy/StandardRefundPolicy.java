package ar.edu.uade.skillswap.domain.policy;

import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/** 100% with 48+ hours notice, 50% with 24-48 hours, otherwise 0%. */
public final class StandardRefundPolicy implements RefundPolicy {
    @Override public SkillCoins refundFor(Booking booking, LocalDateTime cancelledAt, LocalDateTime sessionStartsAt) {
        Objects.requireNonNull(booking, "Falta contratación");
        Objects.requireNonNull(cancelledAt, "Falta la fecha de cancelación");
        Objects.requireNonNull(sessionStartsAt, "Falta el inicio de sesión");
        long hours = Duration.between(cancelledAt, sessionStartsAt).toHours();
        int percent = hours >= 48 ? 100 : hours >= 24 ? 50 : 0;
        return new SkillCoins((int) ((long) booking.price().amount() * percent / 100));
    }
}
