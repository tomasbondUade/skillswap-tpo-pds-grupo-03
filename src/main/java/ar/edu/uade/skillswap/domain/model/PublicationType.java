package ar.edu.uade.skillswap.domain.model;

/**
 * Tipo de publicación (consigna §4.2). Los tres tipos comparten datos, estados y flujo de
 * contratación; difieren en qué tipo de actor puede crearlos (ver {@link ActorType}).
 */
public enum PublicationType {
    /** Mentoría de un profesional. */
    MENTORSHIP,
    /** Servicio de un profesional. */
    SERVICE,
    /** Desafío de una organización. */
    CHALLENGE
}
