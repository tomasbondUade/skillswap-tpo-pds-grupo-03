package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.error.ApplicationErrorCode;
import ar.edu.uade.skillswap.application.error.ApplicationException;
import ar.edu.uade.skillswap.application.mapper.PublicationApplicationMapper;
import ar.edu.uade.skillswap.application.model.PublicationData.ChangeStatusCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.CreateDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.EditDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.application.port.in.PublicationUseCases;
import ar.edu.uade.skillswap.application.port.out.CurrentUserProvider;
import ar.edu.uade.skillswap.application.port.out.PublicationRepository;
import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import ar.edu.uade.skillswap.domain.model.Publication;
import ar.edu.uade.skillswap.domain.model.PublicationStatus;
import ar.edu.uade.skillswap.domain.model.SkillCoins;
import java.util.List;
import java.util.Objects;

/** Coordinates publication commands and delegates all lifecycle rules to Publication. */
public final class PublicationApplicationService implements PublicationUseCases {
    private final PublicationRepository publications;
    private final CurrentUserProvider currentUser;
    private final TransactionManager transactions;

    public PublicationApplicationService(PublicationRepository publications, CurrentUserProvider currentUser,
                                         TransactionManager transactions) {
        this.publications = Objects.requireNonNull(publications);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public PublicationView createDraft(CreateDraftCommand command) {
        Objects.requireNonNull(command, "Faltan los datos de la publicación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        return transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            var builder = Publication.builder(actor)
                    .title(command.title())
                    .description(command.description())
                    .category(command.category())
                    .modality(command.modality())
                    .durationMinutes(command.durationMinutes())
                    .capacity(command.capacity())
                    .basePrice(new SkillCoins(command.priceInSkillCoins()))
                    .tags(command.tags());
            PublicationApplicationMapper.toAvailabilitySlots(command.availability()).forEach(builder::addAvailability);
            Publication publication = builder.build();
            return PublicationApplicationMapper.toView(publications.save(publication));
        }));
    }

    @Override
    public PublicationView editDraft(EditDraftCommand command) {
        Objects.requireNonNull(command, "Faltan los datos para editar la publicación");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String publicationId = UseCaseSupport.requiredId(command.publicationId(), "publicación");
        return transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Publication publication = publications.findByIdForUpdate(publicationId)
                    .orElseThrow(() -> notFound("No existe la publicación indicada"));
            publication.edit(actor, command.title(), command.description(), command.category(), command.modality(),
                    command.durationMinutes(), command.capacity(), command.tags());
            return PublicationApplicationMapper.toView(publications.save(publication));
        }));
    }

    @Override
    public PublicationView changeStatus(ChangeStatusCommand command) {
        Objects.requireNonNull(command, "Faltan los datos del cambio de estado");
        String actor = UseCaseSupport.currentUserId(currentUser);
        String publicationId = UseCaseSupport.requiredId(command.publicationId(), "publicación");
        return transactions.required(() -> UseCaseSupport.translateDomainFailures(() -> {
            Publication publication = publications.findByIdForUpdate(publicationId)
                    .orElseThrow(() -> notFound("No existe la publicación indicada"));
            transition(publication, actor, command.targetStatus(), command.reason());
            return PublicationApplicationMapper.toView(publications.save(publication));
        }));
    }

    @Override
    public List<PublicationView> listMyPublications() {
        String actor = UseCaseSupport.currentUserId(currentUser);
        return publications.findByAuthorUserId(actor).stream()
                .map(PublicationApplicationMapper::toView)
                .toList();
    }

    private static void transition(Publication publication, String actor,
                                   PublicationStatus targetStatus, String reason) {
        if (targetStatus == null) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION,
                    "Falta el estado de destino");
        }
        switch (targetStatus) {
            case PUBLISHED -> publication.publish(actor);
            case PAUSED -> publication.pause(actor);
            case CLOSED -> publication.close(actor);
            case CANCELLED -> publication.cancel(actor, reason);
            case DRAFT -> throw new ApplicationException(ApplicationErrorCode.INVALID_STATE,
                    "No se puede volver al estado de borrador");
        }
    }

    private static ApplicationException notFound(String message) {
        return new ApplicationException(ApplicationErrorCode.NOT_FOUND, message);
    }
}
