package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import java.util.List;

/** Inbound port for public search and personalized discovery. */
public interface DiscoveryUseCases {
    PageResult<PublicationView> search(SearchQuery query);
    List<PublicationView> recommend(int limit);
    List<String> categories();
}
