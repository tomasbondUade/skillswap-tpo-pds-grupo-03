package ar.edu.uade.skillswap.application.port.out;

import ar.edu.uade.skillswap.application.model.NotificationMessage;

/** Output port for notifications; adapters select and invoke external delivery clients. */
public interface NotificationGateway {
    void send(NotificationMessage notification);
}
