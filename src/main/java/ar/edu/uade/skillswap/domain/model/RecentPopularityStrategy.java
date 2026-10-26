package ar.edu.uade.skillswap.domain.model;

import java.util.Comparator;
import java.util.List;

/** Ranks published offers by request activity, breaking ties with recency. */
public final class RecentPopularityStrategy implements RecommendationStrategy {
    @Override public List<Publication> recommend(Profile profile, List<Publication> candidates, int limit) {
        java.util.Objects.requireNonNull(profile, "Falta perfil");
        if (limit < 0) throw new IllegalArgumentException("El límite no puede ser negativo");
        if (limit == 0 || !profile.active()) return List.of();
        return new java.util.ArrayList<>(java.util.Objects.requireNonNull(candidates, "Faltan publicaciones candidatas"))
                .stream().filter(java.util.Objects::nonNull).filter(Publication::acceptsRequests)
                .sorted(Comparator.comparing(Publication::lastRequestedAt,Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Comparator.comparingInt(Publication::requestCount).reversed())
                        .thenComparing(Publication::createdAt, Comparator.reverseOrder()))
                .limit(limit).toList();
    }
    @Override public String name() { return "Popularidad reciente"; }
}
