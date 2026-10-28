package ar.edu.uade.skillswap.infrastructure;

import ar.edu.uade.skillswap.application.model.BookingData.BookingCommand;
import ar.edu.uade.skillswap.application.model.BookingData.RequestCommand;
import ar.edu.uade.skillswap.application.model.RatingData.SubmitCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.domain.model.NotificationChannelType;
import ar.edu.uade.skillswap.domain.model.Profile;
import ar.edu.uade.skillswap.infrastructure.identity.MockIdentityClient;
import ar.edu.uade.skillswap.infrastructure.notification.email.MockEmailClient;
import ar.edu.uade.skillswap.infrastructure.notification.push.MockPushClient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class SkillSwapApplicationWiringTest {
    @Test
    void usaElAdapterDeIdentidadYLaFactoryEnUnaSolicitudReal() {
        var fixture = new Fixture();
        var publication = fixture.publication();

        var booking = fixture.wiring.bookingService().request(new RequestCommand(publication.id(),
                "¿Hay material previo?", "Practicar Java", publication.availability().get(0).id()));

        assertEquals("user-mateo", booking.buyerUserId());
        assertEquals("¿Hay material previo?", booking.clientMessage());
        assertEquals("¿Hay material previo?", fixture.wiring.bookingService()
                .listMyBookings().get(0).clientMessage());
        assertEquals(List.of("mentor-ana|Nueva solicitud de contratación|Nueva solicitud de contratación"),
                fixture.emailCalls);
        assertEquals(fixture.emailCalls, fixture.pushCalls);
        assertEquals(1, fixture.wiring.notificationService().listMine().size()); // bienvenida

        fixture.identity.actor = "mentor-ana";
        assertEquals("mentor-ana", fixture.wiring.profileService().viewMyProfile().userId());
        var inbox = fixture.wiring.notificationService().listMine();
        assertEquals(1, inbox.size());
        assertEquals("booking.requested", inbox.get(0).type());
        fixture.wiring.notificationService().markRead(inbox.get(0).id());
        assertTrue(fixture.wiring.notificationService().listMine().get(0).read());
    }

    @Test
    void notificaUnaCalificacionEnLosTresCanalesDesdeElFlujoCompleto() {
        var fixture = new Fixture();
        var publication = fixture.publication();
        var booking = fixture.wiring.bookingService().request(new RequestCommand(publication.id(),
                "Mensaje", "Aprender Java", publication.availability().get(0).id()));
        var command = new BookingCommand(booking.id());
        fixture.identity.actor = "mentor-ana";
        fixture.wiring.bookingService().accept(command);
        fixture.identity.actor = "user-mateo";
        fixture.wiring.bookingService().pay(command);
        fixture.identity.actor = "mentor-ana";
        fixture.wiring.bookingService().start(command);
        fixture.identity.actor = "user-mateo";
        fixture.wiring.bookingService().complete(command);

        fixture.wiring.ratingService().submit(new SubmitCommand(booking.id(), 5, "Excelente"));

        assertEquals(4, fixture.emailCalls.size()); // solicitud, pago, completada y calificación
        assertEquals(fixture.emailCalls, fixture.pushCalls);
        assertTrue(fixture.emailCalls.get(3).contains("mentor-ana|Recibiste una nueva calificación|"));
        assertTrue(fixture.emailCalls.get(3).contains("5/5"));
        assertTrue(fixture.emailCalls.get(3).contains("Excelente"));
        fixture.identity.actor = "mentor-ana";
        var inbox = fixture.wiring.notificationService().listMine();
        assertEquals(4, inbox.size());
        assertEquals(1, inbox.stream().filter(item -> "RATING_RECEIVED".equals(item.type())).count());
        assertEquals(5.0, fixture.wiring.profileService().viewMyProfile().reputation());
    }

    private static class Fixture {
        private final MutableIdentityClient identity = new MutableIdentityClient();
        private final List<String> emailCalls = new ArrayList<>();
        private final List<String> pushCalls = new ArrayList<>();
        private final SkillSwapApplicationWiring wiring;

        Fixture() {
            var profiles = new InMemoryProfileRepository();
            profiles.save(Profile.register("mentor-ana", "Ana", "", "UTC", List.of("Java"),
                    List.of(), Set.of(NotificationChannelType.EMAIL, NotificationChannelType.PUSH,
                            NotificationChannelType.IN_APP), List.of()));
            var email = new MockEmailClient() {
                @Override
                public void sendEmail(String recipient, String subject, String body) {
                    emailCalls.add(recipient + "|" + subject + "|" + body);
                }
            };
            var push = new MockPushClient() {
                @Override
                public void sendPush(String recipient, String title, String message) {
                    pushCalls.add(recipient + "|" + title + "|" + message);
                }
            };
            wiring = new SkillSwapApplicationWiring(identity, email, push, profiles);
        }

        PublicationView publication() {
            identity.actor = "mentor-ana";
            var publication = wiring.publicationService().listMyPublications().get(0);
            identity.actor = "user-mateo";
            return publication;
        }
    }

    private static class MutableIdentityClient extends MockIdentityClient {
        private String actor = "user-mateo";

        @Override
        public IdentityResponse getAuthenticatedIdentity() {
            return new IdentityResponse(actor);
        }
    }
}
