package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationSearchContract;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import java.util.List;

/** Ejecuta el contrato de búsqueda contra el repositorio en memoria. */
class InMemoryPublicationSearchTest extends PublicationSearchContract {
    @Override
    protected PublicationRepository createRepository(List<Publication> publications, List<Profile> profiles) {
        var profileRepository = new InMemoryProfileRepository();
        profiles.forEach(profileRepository::save);
        var publicationRepository = new InMemoryPublicationRepository(profileRepository);
        publications.forEach(publicationRepository::save);
        return publicationRepository;
    }
}
