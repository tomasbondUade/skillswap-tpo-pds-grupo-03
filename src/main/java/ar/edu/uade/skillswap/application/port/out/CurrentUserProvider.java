package ar.edu.uade.skillswap.application.port.out;

/** Supplies the authenticated actor without coupling Application to a security framework. */
public interface CurrentUserProvider {
    String currentUserId();
}
