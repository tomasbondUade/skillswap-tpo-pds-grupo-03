package ar.edu.uade.skillswap.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración de JPA e Hibernate para SkillSwap.
 *
 * Lee los datos de conexión desde variables de entorno.
 * No abre la conexión hasta que se solicita la creación
 * del EntityManagerFactory.
 */
public final class HibernateConfig {

    private static final String PERSISTENCE_UNIT =
            "skillswap-persistence";

    private HibernateConfig() {
        // Evita instanciar esta clase de configuración.
    }

    public static EntityManagerFactory createEntityManagerFactory() {
        Map<String, Object> properties = new HashMap<>();

        properties.put(
                "jakarta.persistence.jdbc.driver",
                "com.mysql.cj.jdbc.Driver"
        );

        properties.put(
                "jakarta.persistence.jdbc.url",
                requireEnvironmentVariable("SKILLSWAP_DB_URL")
        );

        properties.put(
                "jakarta.persistence.jdbc.user",
                requireEnvironmentVariable("SKILLSWAP_DB_USER")
        );

        properties.put(
                "jakarta.persistence.jdbc.password",
                requireEnvironmentVariable("SKILLSWAP_DB_PASSWORD")
        );

        return Persistence.createEntityManagerFactory(
                PERSISTENCE_UNIT,
                properties
        );
    }

    private static String requireEnvironmentVariable(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar la variable de entorno: " + name
            );
        }

        return value;
    }
}
