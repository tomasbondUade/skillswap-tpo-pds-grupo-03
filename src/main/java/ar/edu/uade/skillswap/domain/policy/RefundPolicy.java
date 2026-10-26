package ar.edu.uade.skillswap.domain.policy;

import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.time.LocalDateTime;

/** Strategy for calculating a cancellation refund. */
public interface RefundPolicy { SkillCoins refundFor(Booking booking, LocalDateTime cancelledAt, LocalDateTime sessionStartsAt); }
