package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.mapper.PublicationApplicationMapper;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.port.in.DiscoveryUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.ProfileRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.RecommendationStrategy;
import java.util.List;
import java.util.Objects;

/** Orchestrates search and personalized discovery through application ports. */
public final class DiscoveryApplicationService implements DiscoveryUseCases {
    private final PublicationRepository publications;
    private final ProfileRepository profiles;
    private final CurrentUserProvider currentUser;
    private final RecommendationStrategy recommendationStrategy;

    public DiscoveryApplicationService(PublicationRepository publications, ProfileRepository profiles,
                                       CurrentUserProvider currentUser,
                                       RecommendationStrategy recommendationStrategy) {
        this.publications = Objects.requireNonNull(publications);
        this.profiles = Objects.requireNonNull(profiles);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.recommendationStrategy = Objects.requireNonNull(recommendationStrategy);
    }

    @Override
    public PageResult<PublicationView> search(SearchQuery query) {
        Objects.requireNonNull(query, "Faltan los criterios de búsqueda");
        PageResult<Publication> result = publications.searchPublished(query);
        List<PublicationView> items = result.items().stream()
                .map(PublicationApplicationMapper::toView)
                .toList();
        return new PageResult<>(items, result.page(), result.size(), result.totalElements());
    }

    @Override
    public List<PublicationView> recommend(int limit) {
        if (limit < 1 || limit > 100) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION,
                    "El límite de recomendaciones debe estar entre 1 y 100");
        }
        String actor = UseCaseSupport.currentUserId(currentUser);
        Profile profile = profiles.findByUserId(actor)
                .orElseThrow(() -> new ApplicationException(ApplicationErrorCode.NOT_FOUND,
                        "No existe un perfil para el usuario autenticado"));
        if (!profile.active()) {
            throw new ApplicationException(ApplicationErrorCode.INVALID_STATE,
                    "El perfil debe estar activo para recibir recomendaciones");
        }
        List<PublicationView> recommendations = recommendationStrategy
                .recommend(profile, publications.findPublished(), limit).stream()
                .map(PublicationApplicationMapper::toView)
                .toList();
        return recommendations;
    }

    @Override
    public List<String> categories() {
        return publications.findPublished().stream()
                .map(Publication::category)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
