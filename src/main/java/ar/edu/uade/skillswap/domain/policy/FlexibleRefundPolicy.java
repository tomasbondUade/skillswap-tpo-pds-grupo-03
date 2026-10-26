package ar.edu.uade.skillswap.domain.policy;

import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/** Alternative strategy: full refund with 24+ hours notice, half otherwise. */
public final class FlexibleRefundPolicy implements RefundPolicy {
    @Override public SkillCoins refundFor(Booking booking, LocalDateTime cancelledAt, LocalDateTime sessionStartsAt) {
        Objects.requireNonNull(booking, "Falta contratación");
        Objects.requireNonNull(cancelledAt, "Falta la fecha de cancelación");
        Objects.requireNonNull(sessionStartsAt, "Falta el inicio de sesión");
        int percent = Duration.between(cancelledAt, sessionStartsAt).toHours() >= 24 ? 100 : 50;
        return new SkillCoins((int) ((long) booking.price().amount() * percent / 100));
    }
}
