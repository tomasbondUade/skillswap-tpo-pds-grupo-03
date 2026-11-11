package ar.edu.uade.skillswap.domain.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Tipo de actor titular de un perfil. Concentra la única regla que distingue a los actores:
 * qué tipos de publicación puede crear cada uno (consigna §4.2). Agregar un tipo de publicación
 * o habilitarlo para otro actor solo cambia este enum.
 */
public enum ActorType {
    PROFESSIONAL(PublicationType.MENTORSHIP, PublicationType.SERVICE),
    ORGANIZATION(PublicationType.CHALLENGE);

    private final Set<PublicationType> publicationTypes;

    ActorType(PublicationType first, PublicationType... others) {
        this.publicationTypes = Collections.unmodifiableSet(EnumSet.of(first, others));
    }

    /** Tipos de publicación que puede crear este actor, en el orden en que se declaran. */
    public Set<PublicationType> publicationTypes() {
        return publicationTypes;
    }

    public boolean canPublish(PublicationType type) {
        return publicationTypes.contains(type);
    }

    /** Tipo que se usa cuando el autor no elige uno: el primero que puede crear. */
    public PublicationType defaultPublicationType() {
        return publicationTypes.iterator().next();
    }
}
