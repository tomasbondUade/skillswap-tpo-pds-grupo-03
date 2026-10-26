package ar.edu.uade.skillswap.application.port.in;

import ar.edu.uade.skillswap.application.model.ProfileData.ProfileView;
import ar.edu.uade.skillswap.application.model.ProfileData.RegisterCommand;
import ar.edu.uade.skillswap.application.model.ProfileData.UpdateCommand;

/** Inbound port for profile operations exposed by the Application layer. */
public interface ProfileUseCases {
    ProfileView register(RegisterCommand command);
    ProfileView update(UpdateCommand command);
    ProfileView viewMyProfile();
    void deactivateMyProfile();
}
