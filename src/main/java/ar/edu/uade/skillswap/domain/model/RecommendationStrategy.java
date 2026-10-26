package ar.edu.uade.skillswap.domain.model;

import java.util.List;

/** Strategy interface for recommendation algorithms. */
public interface RecommendationStrategy {
    List<Publication> recommend(Profile profile, List<Publication> candidates, int limit);
    String name();
}
