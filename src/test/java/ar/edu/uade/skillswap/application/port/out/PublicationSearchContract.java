package ar.edu.uade.skillswap.application.port.out;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.domain.state.PublicationState;
import ar.edu.uade.skillswap.domain.state.PublicationStates;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Contrato ejecutable de {@link PublicationRepository#searchPublished}. Cada implementación
 * (en memoria, JPA) lo extiende e implementa {@link #createRepository}, que debe dejar persistidos
 * exactamente los datos recibidos. Si una implementación no pasa este contrato, la búsqueda se
 * comporta distinto según el almacenamiento.
 */
public abstract class PublicationSearchContract {
    protected static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 1, 10, 0);

    /** Crea el repositorio bajo prueba con exactamente los datos indicados ya persistidos. */
    protected abstract PublicationRepository createRepository(List<Publication> publications, List<Profile> profiles);

    /**
     * Como en la base (FK), todo autor tiene perfil: completa con un perfil activo y sin
     * calificaciones a los autores que el test no define explícitamente.
     */
    private PublicationRepository repositoryWith(List<Publication> publications, List<Profile> profiles) {
        Set<String> defined = new HashSet<>();
        profiles.forEach(profile -> defined.add(profile.userId()));
        List<Profile> all = new ArrayList<>(profiles);
        publications.stream().map(Publication::authorUserId).distinct()
                .filter(author -> !defined.contains(author))
                .forEach(author -> all.add(profile(author, 0, 0, true)));
        return createRepository(publications, all);
    }

    // ---------- Visibilidad (R1, R2) ----------

    @Test
    void listaSoloPublicacionesPublicadas() {
        var repository = repositoryWith(List.of(
                pub("draft").state(PublicationStates.draft()).build(),
                pub("published").build(),
                pub("paused").state(PublicationStates.paused()).build(),
                pub("closed").state(PublicationStates.closed()).build(),
                pub("cancelled").state(PublicationStates.cancelled()).build()), List.of());

        assertEquals(List.of("published"), ids(repository.searchPublished(query().build())));
    }

    @Test
    void excluyeAutoresDadosDeBajaYSinPerfil() {
        var repository = createRepository(List.of(
                pub("active").author("ana").build(),
                pub("inactive").author("beto").build(),
                pub("no-profile").author("org-1").build()),
                List.of(profile("ana", 0, 0, true), profile("beto", 0, 0, false)));

        assertEquals(List.of("active"), ids(repository.searchPublished(query().build())));
    }

    // ---------- Filtros (R3–R7) ----------

    @Test
    void buscaTextoEnTituloDescripcionCategoriaYEtiquetasSinDistinguirMayusculasNiAcentos() {
        var repository = repositoryWith(List.of(
                pub("title").title("Diseño de APIs").build(),
                pub("description").description("Te enseño diseño de producto").build(),
                pub("category").category("Diseño").build(),
                pub("tag").tags("DISEÑO").build(),
                pub("none").title("Java").build()), List.of());

        assertEquals(Set.of("title", "description", "category", "tag"),
                Set.copyOf(ids(repository.searchPublished(text("diseno")))));
        assertEquals(Set.of("title", "description", "category", "tag"),
                Set.copyOf(ids(repository.searchPublished(text("DISEÑO")))));
    }

    @Test
    void elTextoSeBuscaComoFraseCompleta() {
        var repository = repositoryWith(List.of(
                pub("phrase").title("Java avanzado para backend").build(),
                pub("words").title("Avanzado de Java").build()), List.of());

        assertEquals(List.of("phrase"), ids(repository.searchPublished(text("java avanzado"))));
    }

    @Test
    void textoCategoriaYHabilidadEnBlancoNoFiltran() {
        var repository = repositoryWith(List.of(pub("a").category("Diseño").build(),
                pub("b").category("Tecnología").tags("Java").build()), List.of());

        var blank = new SearchQuery("   ", "  ", null, " ", null, null, null, null, 0, 20);

        assertEquals(2, repository.searchPublished(blank).totalElements());
    }

    @Test
    void filtraPorCategoriaExactaSinDistinguirMayusculas() {
        var repository = repositoryWith(List.of(
                pub("exact").category("Tecnología").build(),
                pub("longer").category("Tecnología aplicada").build(),
                pub("other").category("Diseño").build()), List.of());

        assertEquals(List.of("exact"), ids(repository.searchPublished(query().category("tecnología").build())));
    }

    @Test
    void filtraPorModalidad() {
        var repository = repositoryWith(List.of(
                pub("remote").modality(Modality.REMOTE).build(),
                pub("in-person").modality(Modality.IN_PERSON).build(),
                pub("hybrid").modality(Modality.HYBRID).build()), List.of());

        assertEquals(List.of("in-person"), ids(repository.searchPublished(query().modality(Modality.IN_PERSON).build())));
    }

    @Test
    void filtraPorHabilidadComoIgualdadConUnaEtiqueta() {
        var repository = repositoryWith(List.of(
                pub("java").tags("Java", "Backend").build(),
                pub("javascript").tags("JavaScript").build(),
                pub("author-skill-only").author("ana").title("Fotografía").tags("Retrato").build()),
                List.of(profileWithSkills("ana", Set.of("Java"))));

        assertEquals(List.of("java"), ids(repository.searchPublished(query().skill("java").build())));
    }

    @Test
    void filtraPorRangoDePrecioBaseConLimitesInclusivos() {
        var repository = repositoryWith(List.of(
                pub("p99").price(99).build(), pub("p100").price(100).build(),
                pub("p150").price(150).build(), pub("p200").price(200).build(),
                pub("p201").price(201).build()), List.of());

        assertEquals(Set.of("p100", "p150", "p200"),
                Set.copyOf(ids(repository.searchPublished(query().price(100, 200).build()))));
        assertEquals(Set.of("p99", "p100"),
                Set.copyOf(ids(repository.searchPublished(query().price(null, 100).build()))));
    }

    @Test
    void filtraPorReputacionMinimaInclusivaDelAutor() {
        var repository = repositoryWith(List.of(
                pub("high").author("ana").build(),
                pub("limit").author("beto").build(),
                pub("low").author("caro").build(),
                pub("unrated").author("org-1").build()),
                List.of(profile("ana", 4.8, 5, true), profile("beto", 4.0, 2, true),
                        profile("caro", 3.9, 3, true)));

        assertEquals(Set.of("high", "limit"),
                Set.copyOf(ids(repository.searchPublished(query().minimumReputation(4.0).build()))));
        assertEquals(4, repository.searchPublished(query().minimumReputation(0.0).build()).totalElements());
    }

    @Test
    void combinaTodosLosFiltros() {
        var repository = repositoryWith(List.of(
                pub("match").author("ana").title("Entrevistas Java").category("Tecnología")
                        .modality(Modality.REMOTE).tags("Java").price(150).build(),
                pub("wrong-modality").author("ana").title("Entrevistas Java").category("Tecnología")
                        .modality(Modality.HYBRID).tags("Java").price(150).build(),
                pub("wrong-price").author("ana").title("Entrevistas Java").category("Tecnología")
                        .modality(Modality.REMOTE).tags("Java").price(400).build(),
                pub("wrong-skill").author("ana").title("Entrevistas Java").category("Tecnología")
                        .modality(Modality.REMOTE).tags("Kotlin").price(150).build(),
                pub("low-reputation").author("beto").title("Entrevistas Java").category("Tecnología")
                        .modality(Modality.REMOTE).tags("Java").price(150).build()),
                List.of(profile("ana", 4.5, 4, true), profile("beto", 2.0, 1, true)));

        var query = new SearchQuery("entrevistas", "tecnología", Modality.REMOTE, "java",
                100, 200, 4.0, SearchOrder.RELEVANCE, 0, 20);

        assertEquals(List.of("match"), ids(repository.searchPublished(query)));
    }

    // ---------- Orden (R8) ----------

    @Test
    void relevanciaOrdenaPorPopularidadAunqueHayaTexto() {
        var repository = repositoryWith(List.of(
                pub("title-match").title("Kotlin desde cero").requests(0).build(),
                pub("description-match").description("Incluye kotlin").requests(7).build(),
                pub("tag-match").tags("Kotlin").requests(3).build()), List.of());

        assertEquals(List.of("description-match", "tag-match", "title-match"),
                ids(repository.searchPublished(text("kotlin"))));
    }

    @Test
    void relevanciaSinTextoOrdenaPorPopularidadYLuegoPorFecha() {
        var repository = repositoryWith(List.of(
                pub("a").requests(1).created(T0.plusDays(5)).build(),
                pub("b").requests(9).created(T0).build(),
                pub("c").requests(9).created(T0.plusDays(1)).build()), List.of());

        assertEquals(List.of("c", "b", "a"), ids(repository.searchPublished(order(SearchOrder.RELEVANCE))));
    }

    @Test
    void mejorReputacionOrdenaPorReputacionCalificacionesYFecha() {
        var repository = repositoryWith(List.of(
                pub("unrated").author("org-1").build(),
                pub("ana-old").author("ana").created(T0).build(),
                pub("ana-new").author("ana").created(T0.plusDays(1)).build(),
                pub("beto").author("beto").build(),
                pub("caro").author("caro").build()),
                List.of(profile("ana", 4.5, 10, true), profile("beto", 4.5, 3, true),
                        profile("caro", 4.9, 1, true)));

        assertEquals(List.of("caro", "ana-new", "ana-old", "beto", "unrated"),
                ids(repository.searchPublished(order(SearchOrder.BEST_RATING))));
    }

    @Test
    void menorPrecioOrdenaAscendenteYDesempataPorFecha() {
        var repository = repositoryWith(List.of(
                pub("p300").price(300).build(),
                pub("p100-old").price(100).created(T0).build(),
                pub("p100-new").price(100).created(T0.plusDays(1)).build(),
                pub("free").price(0).build()), List.of());

        assertEquals(List.of("free", "p100-new", "p100-old", "p300"),
                ids(repository.searchPublished(order(SearchOrder.LOWEST_PRICE))));
    }

    @Test
    void masRecienteOrdenaPorFechaDeCreacionDescendente() {
        var repository = repositoryWith(List.of(
                pub("old").created(T0).requests(99).build(),
                pub("newest").created(T0.plusDays(3)).build(),
                pub("middle").created(T0.plusDays(1)).build()), List.of());

        assertEquals(List.of("newest", "middle", "old"), ids(repository.searchPublished(order(SearchOrder.MOST_RECENT))));
    }

    @Test
    void todosLosOrdenesDesempatanPorIdAscendente() {
        List<String> insertionOrder = List.of("zeta", "alfa", "mike", "bravo", "yankee", "charlie",
                "xray", "delta", "kilo", "echo", "papa", "golf");
        var repository = repositoryWith(insertionOrder.stream().map(id -> pub(id).build()).toList(), List.of());
        List<String> expected = insertionOrder.stream().sorted().toList();

        for (SearchOrder order : SearchOrder.values()) {
            assertEquals(expected, ids(repository.searchPublished(order(order))), order.name());
        }
    }

    // ---------- Paginación (R9) ----------

    @Test
    void paginaSinSolaparNiOmitirElementosYConElTotalFiltrado() {
        List<Publication> publications = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            publications.add(pub("p" + index).requests(index).build());
        }
        publications.add(pub("draft").state(PublicationStates.draft()).build());
        var repository = repositoryWith(publications, List.of());

        List<String> seen = new ArrayList<>();
        int[] expectedSizes = {3, 3, 1, 0};
        for (int page = 0; page < expectedSizes.length; page++) {
            PageResult<Publication> result = repository.searchPublished(page(page, 3));
            assertEquals(page, result.page());
            assertEquals(3, result.size());
            assertEquals(7, result.totalElements());
            assertEquals(3, result.totalPages());
            assertEquals(expectedSizes[page], result.items().size());
            seen.addAll(ids(result));
        }

        assertEquals(List.of("p6", "p5", "p4", "p3", "p2", "p1", "p0"), seen);
        assertEquals(7, new HashSet<>(seen).size());
    }

    @Test
    void sinResultadosDevuelvePaginaVaciaConTotalCero() {
        var repository = repositoryWith(List.of(pub("a").build()), List.of());

        PageResult<Publication> result = repository.searchPublished(text("inexistente"));

        assertTrue(result.items().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    // ---------- Fixtures ----------

    protected static List<String> ids(PageResult<Publication> result) {
        return result.items().stream().map(Publication::id).toList();
    }

    protected static SearchQuery text(String text) {
        return query().text(text).build();
    }

    protected static SearchQuery order(SearchOrder order) {
        return query().order(order).build();
    }

    protected static SearchQuery page(int page, int size) {
        return query().page(page, size).build();
    }

    protected static QueryFixture query() {
        return new QueryFixture();
    }

    protected static PublicationFixture pub(String id) {
        return new PublicationFixture(id);
    }

    protected static Profile profile(String userId, double reputation, int ratingCount, boolean active) {
        return new Profile(null, userId, "Nombre " + userId, "", "UTC", Set.of(), Set.of(), Set.of(),
                reputation, ratingCount, active);
    }

    protected static Profile profileWithSkills(String userId, Set<String> skills) {
        return new Profile(null, userId, "Nombre " + userId, "", "UTC", skills, Set.of(), Set.of(),
                0, 0, true);
    }

    /** Valores por defecto neutros: ningún filtro de los tests los incluye por accidente. */
    protected static final class PublicationFixture {
        private final String id;
        private String author = "provider";
        private String title;
        private String description;
        private String category = "General";
        private Modality modality = Modality.REMOTE;
        private int price = 100;
        private Set<String> tags = Set.of();
        private PublicationState state = PublicationStates.published();
        private LocalDateTime created = T0;
        private int requests;

        private PublicationFixture(String id) {
            this.id = id;
            this.title = "Mentoría " + id;
            this.description = "Sesión " + id;
        }

        public PublicationFixture author(String value) { author = value; return this; }
        public PublicationFixture title(String value) { title = value; return this; }
        public PublicationFixture description(String value) { description = value; return this; }
        public PublicationFixture category(String value) { category = value; return this; }
        public PublicationFixture modality(Modality value) { modality = value; return this; }
        public PublicationFixture price(int value) { price = value; return this; }
        public PublicationFixture tags(String... values) { tags = Set.of(values); return this; }
        public PublicationFixture state(PublicationState value) { state = value; return this; }
        public PublicationFixture created(LocalDateTime value) { created = value; return this; }
        public PublicationFixture requests(int value) { requests = value; return this; }

        public Publication build() {
            return new Publication(id, author, title, description, category, modality, 60, 5,
                    new SkillCoins(price), tags, List.of(), state, created, requests,
                    requests == 0 ? null : created, 0, List.of());
        }
    }

    protected static final class QueryFixture {
        private String text;
        private String category;
        private Modality modality;
        private String skill;
        private Integer minimumPrice;
        private Integer maximumPrice;
        private Double minimumReputation;
        private SearchOrder order;
        private int page;
        private int size = 20;

        public QueryFixture text(String value) { text = value; return this; }
        public QueryFixture category(String value) { category = value; return this; }
        public QueryFixture modality(Modality value) { modality = value; return this; }
        public QueryFixture skill(String value) { skill = value; return this; }
        public QueryFixture price(Integer minimum, Integer maximum) {
            minimumPrice = minimum;
            maximumPrice = maximum;
            return this;
        }
        public QueryFixture minimumReputation(double value) { minimumReputation = value; return this; }
        public QueryFixture order(SearchOrder value) { order = value; return this; }
        public QueryFixture page(int number, int pageSize) {
            page = number;
            size = pageSize;
            return this;
        }

        public SearchQuery build() {
            return new SearchQuery(text, category, modality, skill, minimumPrice, maximumPrice, minimumReputation, order,
                    page, size);
        }
    }
}
