package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchOrder;
import ar.edu.uade.skillswap.application.model.DiscoveryData.SearchQuery;
import ar.edu.uade.skillswap.application.model.PageResult;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.StatusHistoryEntry;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaRepositorySupport;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.ProfileEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationAvailabilityEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationStatusHistoryEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.entity.PublicationTagEntity;
import ar.edu.uade.skillswap.infrastructure.persistence.mapper.PublicationMapper;

import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class JpaPublicationRepository extends JpaRepositorySupport
        implements PublicationRepository {

    public JpaPublicationRepository(JpaTransactionManager transactions) {
        super(transactions);
    }

    @Override
    public Optional<Publication> findById(String publicationId) {
        Objects.requireNonNull(publicationId, "Falta el ID de la publicación");

        return query(em -> Optional.ofNullable(
                        em.find(PublicationEntity.class, publicationId))
                .map(PublicationMapper::toDomain));
    }

    @Override
    public Optional<Publication> findByIdForUpdate(String publicationId) {
        Objects.requireNonNull(publicationId, "Falta el ID de la publicación");

        return query(em -> {
            PublicationEntity entity = em.find(PublicationEntity.class,
                    publicationId, LockModeType.PESSIMISTIC_WRITE);
            if (entity == null) return Optional.empty();

            // Under MySQL REPEATABLE READ a prior booking/history read may have
            // established an old snapshot. Locking the parent alone does not
            // refresh its availability rows: read and lock the actual counters.
            em.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            em.createQuery("SELECT s FROM PublicationAvailabilityEntity s "
                            + "WHERE s.publication.id = :id ORDER BY s.id",
                            PublicationAvailabilityEntity.class)
                    .setParameter("id", publicationId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList()
                    .forEach(slot -> em.refresh(slot, LockModeType.PESSIMISTIC_WRITE));
            return Optional.of(PublicationMapper.toDomain(entity));
        });
    }

    @Override
    public List<Publication> findByAuthorUserId(String authorUserId) {
        Objects.requireNonNull(authorUserId, "Falta el ID del autor");

        return query(em -> em.createQuery(
                        "SELECT p FROM PublicationEntity p "
                                + "WHERE p.authorUserId = :author "
                                + "ORDER BY p.createdAt DESC",
                        PublicationEntity.class)
                .setParameter("author", authorUserId)
                .getResultList()
                .stream()
                .map(PublicationMapper::toDomain)
                .toList());
    }

    @Override
    public List<Publication> findPublished() {
        return query(em -> em.createQuery(
                        "SELECT p FROM PublicationEntity p "
                                + "WHERE p.status = :status "
                                + "ORDER BY p.createdAt DESC",
                        PublicationEntity.class)
                .setParameter("status", "PUBLISHED")
                .getResultList()
                .stream()
                .map(PublicationMapper::toDomain)
                .toList());
    }

    @Override
    public PageResult<Publication> searchPublished(SearchQuery search) {
        Objects.requireNonNull(search, "Faltan los criterios de búsqueda");

        return query(em -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();

            CriteriaQuery<PublicationEntity> resultQuery =
                    cb.createQuery(PublicationEntity.class);
            Root<PublicationEntity> publication =
                    resultQuery.from(PublicationEntity.class);
            Root<ProfileEntity> author = resultQuery.from(ProfileEntity.class);
            Predicate[] predicates = buildPredicates(
                    cb, resultQuery, publication, author, search);
            resultQuery.select(publication)
                    .where(predicates)
                    .orderBy(orderBy(cb, publication, author, search.order()));

            int firstResult = Math.multiplyExact(
                    search.page(), search.size());

            List<Publication> items = em.createQuery(resultQuery)
                    .setFirstResult(firstResult)
                    .setMaxResults(search.size())
                    .getResultList()
                    .stream()
                    .map(PublicationMapper::toDomain)
                    .toList();

            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<PublicationEntity> countRoot =
                    countQuery.from(PublicationEntity.class);
            Root<ProfileEntity> countAuthor = countQuery.from(ProfileEntity.class);

            countQuery.select(cb.countDistinct(countRoot))
                    .where(buildPredicates(
                            cb, countQuery, countRoot, countAuthor, search));

            long total = em.createQuery(countQuery).getSingleResult();

            return new PageResult<>(
                    items, search.page(), search.size(), total);
        });
    }

    private Predicate[] buildPredicates(
            CriteriaBuilder cb,
            CriteriaQuery<?> criteria,
            Root<PublicationEntity> publication,
            Root<ProfileEntity> author,
            SearchQuery search) {

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(publication.get("status"), "PUBLISHED"));
        predicates.add(cb.equal(author.get("userId"), publication.get("authorUserId")));
        predicates.add(cb.isTrue(author.get("active")));

        if (!search.text().isBlank()) {
            String pattern = containsPattern(search.text());
            var tagMatches = criteria.subquery(Integer.class);
            Root<PublicationTagEntity> tag = tagMatches.from(PublicationTagEntity.class);
            tagMatches.select(cb.literal(1)).where(
                    cb.equal(tag.get("publication").get("id"), publication.get("id")),
                    cb.like(cb.lower(tag.get("id").get("tag")), pattern, '\\'));

            predicates.add(cb.or(
                    cb.like(cb.lower(publication.get("title")), pattern, '\\'),
                    cb.like(cb.lower(publication.get("description")), pattern, '\\'),
                    cb.like(cb.lower(publication.get("category")), pattern, '\\'),
                    cb.exists(tagMatches)
            ));
        }

        if (search.category() != null && !search.category().isBlank()) {
            predicates.add(cb.equal(
                    cb.lower(publication.get("category")),
                    search.category().toLowerCase(Locale.ROOT)));
        }

        if (search.modality() != null) {
            predicates.add(cb.equal(
                    publication.get("modality"),
                    search.modality().name()));
        }

        if (search.minimumPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    publication.<Integer>get("basePrice"),
                    search.minimumPrice()));
        }

        if (search.maximumPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    publication.<Integer>get("basePrice"),
                    search.maximumPrice()));
        }

        if (search.skill() != null && !search.skill().isBlank()) {
            var skillMatches = criteria.subquery(Integer.class);
            Root<PublicationTagEntity> tag = skillMatches.from(PublicationTagEntity.class);
            skillMatches.select(cb.literal(1)).where(
                    cb.equal(tag.get("publication").get("id"), publication.get("id")),
                    cb.equal(cb.lower(tag.get("id").get("tag")),
                            search.skill().toLowerCase(Locale.ROOT)));
            predicates.add(cb.exists(skillMatches));
        }

        if (search.minimumReputation() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    author.<java.math.BigDecimal>get("reputation"),
                    java.math.BigDecimal.valueOf(search.minimumReputation())));
        }

        return predicates.toArray(Predicate[]::new);
    }

    private List<jakarta.persistence.criteria.Order> orderBy(
            CriteriaBuilder cb,
            Root<PublicationEntity> publication,
            Root<ProfileEntity> author,
            SearchOrder order) {

        List<jakarta.persistence.criteria.Order> orders = new ArrayList<>(switch (order) {
            case LOWEST_PRICE -> List.of(
                    cb.asc(publication.get("basePrice")),
                    cb.desc(publication.get("createdAt")));
            case MOST_RECENT -> List.of(
                    cb.desc(publication.get("createdAt")));
            case BEST_RATING -> List.of(
                    cb.desc(author.get("reputation")),
                    cb.desc(author.get("ratingCount")),
                    cb.desc(publication.get("createdAt")));
            case RELEVANCE -> List.of(
                    cb.desc(publication.get("requestCount")),
                    cb.desc(publication.get("createdAt")));
        });
        // A unique final key keeps page boundaries stable when prices/dates tie.
        orders.add(cb.asc(publication.get("id")));
        return orders;
    }

    private static String containsPattern(String value) {
        String escaped = value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    @Override
    public Publication save(Publication publication) {
        Objects.requireNonNull(publication, "Falta la publicación");

        var em = entityManager();
        PublicationEntity managed = em.find(
                PublicationEntity.class, publication.id());

        if (managed == null) {
            PublicationEntity created =
                    PublicationMapper.toEntity(publication);

            em.persist(created);
            return PublicationMapper.toDomain(created);
        }

        managed.setAuthorUserId(publication.authorUserId());
        managed.setTitle(publication.title());
        managed.setDescription(publication.description());
        managed.setCategory(publication.category());
        managed.setModality(publication.modality().name());
        managed.setDurationMinutes(publication.durationMinutes());
        managed.setCapacity(publication.capacity());
        managed.setBasePrice(publication.basePrice().amount());
        managed.setStatus(publication.status().name());
        managed.setCreatedAt(publication.createdAt());
        managed.setRequestCount(publication.requestCount());
        managed.setLastRequestedAt(publication.lastRequestedAt());

        synchronizeTags(managed, publication.tags());
        synchronizeAvailability(managed, publication.availability());
        synchronizeHistory(managed, publication.history());

        return PublicationMapper.toDomain(managed);
    }

    private void synchronizeTags(
            PublicationEntity managed,
            Set<String> domainTags) {

        Map<String, PublicationTagEntity> existing =
                managed.getTags().stream().collect(Collectors.toMap(
                        tag -> tag.getId().getTag(),
                        Function.identity()));

        Set<String> desired = new HashSet<>(domainTags);

        managed.getTags().removeIf(
                tag -> !desired.contains(tag.getId().getTag()));

        Set<String> current = managed.getTags().stream()
                .map(tag -> tag.getId().getTag())
                .collect(Collectors.toSet());

        for (String tag : desired) {
            if (!current.contains(tag)) {
                PublicationTagEntity entity =
                        new PublicationTagEntity();

                entity.setId(new PublicationTagEntity.PublicationTagId(
                        managed.getId(), tag));

                managed.addTag(entity);
            }
        }
    }

    private void synchronizeAvailability(
            PublicationEntity managed,
            List<AvailabilitySlot> domainSlots) {

        Map<String, PublicationAvailabilityEntity> existing =
                managed.getAvailability().stream().collect(
                        Collectors.toMap(
                                PublicationAvailabilityEntity::getId,
                                Function.identity()));

        Set<String> desiredIds = domainSlots.stream()
                .map(AvailabilitySlot::id)
                .collect(Collectors.toSet());

        managed.getAvailability().removeIf(
                slot -> !desiredIds.contains(slot.getId()));

        Map<String, PublicationAvailabilityEntity> remaining =
                managed.getAvailability().stream().collect(
                        Collectors.toMap(
                                PublicationAvailabilityEntity::getId,
                                Function.identity()));

        for (AvailabilitySlot slot : domainSlots) {
            PublicationAvailabilityEntity entity =
                    remaining.get(slot.id());

            if (entity == null) {
                entity = new PublicationAvailabilityEntity();
                entity.setId(slot.id());
                managed.addAvailability(entity);
            }

            entity.setStartsAt(slot.startsAt());
            entity.setEndsAt(slot.endsAt());
            entity.setCapacity(slot.capacity());
            entity.setReserved(slot.reserved());
        }
    }

    private void synchronizeHistory(
            PublicationEntity managed,
            List<StatusHistoryEntry> domainHistory) {

        Set<String> existingKeys = managed.getHistory().stream()
                .map(this::historyKey)
                .collect(Collectors.toSet());

        for (StatusHistoryEntry entry : domainHistory) {
            String key = historyKey(entry);

            if (existingKeys.add(key)) {
                PublicationStatusHistoryEntity entity =
                        new PublicationStatusHistoryEntity();

                entity.setFromStatus(entry.from());
                entity.setToStatus(entry.to());
                entity.setActorUserId(entry.actorUserId());
                entity.setReason(entry.reason());
                entity.setOccurredAt(entry.occurredAt());

                managed.addHistory(entity);
            }
        }
    }

    private String historyKey(PublicationStatusHistoryEntity entry) {
        return historyKey(new StatusHistoryEntry(
                entry.getFromStatus(),
                entry.getToStatus(),
                entry.getActorUserId(),
                entry.getReason(),
                entry.getOccurredAt()));
    }

    private String historyKey(StatusHistoryEntry entry) {
        return String.join("|",
                Objects.toString(entry.from(), ""),
                entry.to(),
                entry.actorUserId(),
                entry.reason(),
                entry.occurredAt().toString());
    }
}
