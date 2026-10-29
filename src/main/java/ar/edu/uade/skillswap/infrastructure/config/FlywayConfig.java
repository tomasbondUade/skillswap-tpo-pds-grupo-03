package ar.edu.uade.skillswap.infrastructure.config;

import org.flywaydb.core.Flyway;

/**
 * Configura y ejecuta las migraciones de la base de datos.
 */
public final class FlywayConfig {

    private FlywayConfig() {
        // Evita instanciar esta clase de configuración.
    }

    public static void migrate() {
        String url = requireEnvironmentVariable("SKILLSWAP_DB_URL");
        String user = requireEnvironmentVariable("SKILLSWAP_DB_USER");
        String password = requireEnvironmentVariable("SKILLSWAP_DB_PASSWORD");

        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load()
                .migrate();
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
