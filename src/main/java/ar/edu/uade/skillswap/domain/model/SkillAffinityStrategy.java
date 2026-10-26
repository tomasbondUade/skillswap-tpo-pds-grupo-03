package ar.edu.uade.skillswap.domain.model;

import java.util.*;

/** Ranks published offers by overlap with the user's skills and interests. */
public final class SkillAffinityStrategy implements RecommendationStrategy {
    @Override public List<Publication> recommend(Profile profile, List<Publication> candidates, int limit) {
        java.util.Objects.requireNonNull(profile, "Falta perfil");
        if (limit < 0) throw new IllegalArgumentException("El límite no puede ser negativo");
        if (limit == 0 || !profile.active()) return List.of();
        Set<String> interests = new HashSet<>();
        profile.skills().forEach(s -> interests.add(s.toLowerCase(Locale.ROOT)));
        profile.interests().forEach(s -> interests.add(s.toLowerCase(Locale.ROOT)));
        return new ArrayList<>(java.util.Objects.requireNonNull(candidates, "Faltan publicaciones candidatas"))
                .stream().filter(java.util.Objects::nonNull).filter(Publication::acceptsRequests)
                .sorted(Comparator.<Publication>comparingInt(p -> overlap(p, interests)).reversed()
                        .thenComparing(Publication::createdAt, Comparator.reverseOrder()))
                .limit(limit).toList();
    }
    private int overlap(Publication publication, Set<String> interests) {
        int matches = 0;
        for (String tag : publication.tags()) if (interests.contains(tag.toLowerCase(Locale.ROOT))) matches++;
        if (interests.contains(publication.category().toLowerCase(Locale.ROOT))) matches++;
        return matches;
    }
    @Override public String name() { return "Afinidad por habilidades"; }
}
