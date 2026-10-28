package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.RatingRepository;
import ar.edu.uade.skillswap.domain.model.Rating;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryRatingRepository implements RatingRepository {
    private final Map<String, Rating> ratings = new LinkedHashMap<>();

    @Override
    public boolean existsByBookingIdAndAuthorUserId(String bookingId, String authorUserId) {
        return ratings.values().stream().anyMatch(rating -> rating.bookingId().equals(bookingId)
                && rating.authorUserId().equals(authorUserId));
    }

    @Override
    public List<Rating> findBySubjectUserId(String subjectUserId) {
        return ratings.values().stream()
                .filter(rating -> rating.subjectUserId().equals(subjectUserId))
                .toList();
    }

    @Override
    public Rating save(Rating rating) {
        ratings.put(rating.id(), rating);
        return rating;
    }
}