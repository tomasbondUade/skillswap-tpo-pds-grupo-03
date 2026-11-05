package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.application.port.out.PublicationSearchContract;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Ejecuta el contrato común de búsqueda contra un esquema MySQL aislado por caso. */
@EnabledIfSystemProperty(named = "skillswap.mysql.it", matches = "true")
@Timeout(60)
class JpaPublicationSearchContractTest extends PublicationSearchContract {
    private String schema;
    private String baseUrl;
    private String url;
    private String user;
    private String password;
    private EntityManagerFactory factory;
    private JpaTransactionManager transactions;
    private JpaProfileRepository profiles;
    private JpaPublicationRepository publications;

    @BeforeEach
    void createIsolatedSchema() throws Exception {
        baseUrl = requireEnvironment("SKILLSWAP_MYSQL_TEST_URL");
        user = requireEnvironment("SKILLSWAP_MYSQL_TEST_USER");
        password = requireEnvironment("SKILLSWAP_MYSQL_TEST_PASSWORD");
        if (!baseUrl.matches("jdbc:mysql://[^/]+/\\?.+")) {
            throw new IllegalArgumentException("Use jdbc:mysql://host:port/?opciones, sin base de datos");
        }

        schema = "skillswap_search_it_" + UUID.randomUUID().toString().replace("-", "");
        url = baseUrl.replace("/?", "/" + schema + "?");
        try (var connection = DriverManager.getConnection(baseUrl, user, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4");
        }
    }

    @AfterEach
    void removeOnlyOwnedSchema() throws Exception {
        try {
            if (factory != null && factory.isOpen()) factory.close();
        } finally {
            if (schema != null && schema.matches("skillswap_search_it_[a-f0-9]{32}")) {
                try (var connection = DriverManager.getConnection(baseUrl, user, password);
                     var statement = connection.createStatement()) {
                    statement.execute("DROP DATABASE IF EXISTS `" + schema + "`");
                }
            }
        }
    }

    @Override
    protected PublicationRepository createRepository(List<Publication> items, List<Profile> authors) {
        Flyway.configure().dataSource(url, user, password)
                .locations("classpath:db/migration").load().migrate();
        factory = Persistence.createEntityManagerFactory("skillswap-persistence", Map.of(
                "jakarta.persistence.jdbc.driver", "com.mysql.cj.jdbc.Driver",
                "jakarta.persistence.jdbc.url", url,
                "jakarta.persistence.jdbc.user", user,
                "jakarta.persistence.jdbc.password", password,
                "hibernate.hbm2ddl.auto", "validate"));
        transactions = new JpaTransactionManager(factory);
        profiles = new JpaProfileRepository(transactions);
        publications = new JpaPublicationRepository(transactions);

        return transactions.required(() -> {
            authors.forEach(profiles::save);
            items.forEach(publications::save);
            return publications;
        });
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configurar la variable de entorno: " + name);
        }
        return value;
    }
}
