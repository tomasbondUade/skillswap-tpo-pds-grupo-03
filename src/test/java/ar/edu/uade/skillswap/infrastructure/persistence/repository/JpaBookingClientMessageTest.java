package ar.edu.uade.skillswap.infrastructure.persistence.repository;

import ar.edu.uade.skillswap.domain.model.ActorType;
import ar.edu.uade.skillswap.domain.model.AvailabilitySlot;
import ar.edu.uade.skillswap.domain.model.Booking;
import ar.edu.uade.skillswap.domain.model.Modality;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import ar.edu.uade.skillswap.infrastructure.persistence.JpaTransactionManager;
import ar.edu.uade.skillswap.infrastructure.persistence.mapper.PublicationMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JpaBookingClientMessageTest {

    @Test
    void preservesClientMessageAcrossJpaSaveLoadAndUpdate() {
        String databaseName = "booking_message_" + UUID.randomUUID().toString().replace("-", "");
        String jdbcUrl = "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(
                "skillswap-test-persistence",
                Map.of(
                        "jakarta.persistence.jdbc.driver", "org.h2.Driver",
                        "jakarta.persistence.jdbc.url", jdbcUrl,
                        "jakarta.persistence.jdbc.user", "sa",
                        "jakarta.persistence.jdbc.password", "",
                        "hibernate.hbm2ddl.auto", "create-drop",
                        "hibernate.show_sql", "false"
                ));

        try {
            JpaTransactionManager transactions = new JpaTransactionManager(entityManagerFactory);
            JpaBookingRepository bookings = new JpaBookingRepository(transactions);
            LocalDateTime startsAt = LocalDateTime.of(2026, 11, 12, 10, 0);
            AvailabilitySlot slot = AvailabilitySlot.of(startsAt, startsAt.plusHours(1), 2);
            Publication publication = Publication.builder("mentor-1", ActorType.PROFESSIONAL,
                    PublicationType.MENTORSHIP)
                    .title("Mentoría de Java")
                    .description("Práctica individual")
                    .category("Tecnología")
                    .modality(Modality.REMOTE)
                    .durationMinutes(60)
                    .capacity(2)
                    .basePrice(new SkillCoins(100))
                    .addAvailability(slot)
                    .build();
            publication.publish("mentor-1");

            String message = "¿Podemos repasar ejercicios antes de la entrevista?";
            String objective = "Practicar diseño de APIs";
            Booking requested = Booking.request(
                    publication, "student-1", message, objective, slot.id(), publication.basePrice());

            transactions.required(() -> {
                transactions.currentEntityManager().persist(PublicationMapper.toEntity(publication));
                bookings.save(requested);
                return null;
            });

            Booking loaded = bookings.findById(requested.id()).orElseThrow();
            assertEquals(message, loaded.clientMessage());
            assertEquals(objective, loaded.objective());
            assertEquals(slot.id(), loaded.slotId());

            transactions.required(() -> {
                Booking accepted = bookings.findById(requested.id()).orElseThrow();
                accepted.accept("mentor-1");
                bookings.save(accepted);
                return null;
            });

            Booking updated = bookings.findById(requested.id()).orElseThrow();
            assertEquals(message, updated.clientMessage());
        } finally {
            entityManagerFactory.close();
        }
    }

    @Test
    void migrationAddsNullableClientMessageToLegacyBookings() throws Exception {
        String databaseName = "booking_message_migration_"
                + UUID.randomUUID().toString().replace("-", "");
        String jdbcUrl = "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE bookings (id VARCHAR(100) PRIMARY KEY)");
            statement.execute("INSERT INTO bookings (id) VALUES ('legacy-booking')");
            statement.execute(readClientMessageMigration());

            try (ResultSet result = statement.executeQuery(
                    "SELECT client_message FROM bookings WHERE id = 'legacy-booking'")) {
                result.next();
                assertNull(result.getString("client_message"));
            }
        }
    }

    private String readClientMessageMigration() throws IOException {
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V2__add_client_message_to_bookings.sql")) {
            if (stream == null) {
                throw new IllegalStateException("No se encontró la migración V2 de reservas");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
