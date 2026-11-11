package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.PublicationData.ChangeStatusCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.CreateDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.EditDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import ar.edu.uade.skillswap.domain.model.PublicationType;
import java.util.List;

/** Inbound port for the author's publication lifecycle. */
public interface PublicationUseCases {
    PublicationView createDraft(CreateDraftCommand command);
    PublicationView editDraft(EditDraftCommand command);
    PublicationView changeStatus(ChangeStatusCommand command);
    List<PublicationView> listMyPublications();
    /** Tipos de publicación que puede crear el usuario actual, para ofrecerlos al crear un borrador. */
    List<PublicationType> allowedPublicationTypes();
}
