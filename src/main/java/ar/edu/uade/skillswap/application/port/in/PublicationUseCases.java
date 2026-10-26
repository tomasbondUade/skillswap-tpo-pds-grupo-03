package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.PublicationData.ChangeStatusCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.CreateDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.EditDraftCommand;
import ar.edu.uade.skillswap.application.model.PublicationData.PublicationView;
import java.util.List;

/** Inbound port for the author's publication lifecycle. */
public interface PublicationUseCases {
    PublicationView createDraft(CreateDraftCommand command);
    PublicationView editDraft(EditDraftCommand command);
    PublicationView changeStatus(ChangeStatusCommand command);
    List<PublicationView> listMyPublications();
}
