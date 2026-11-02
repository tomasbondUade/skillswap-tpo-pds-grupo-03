package ar.edu.uade.skillswap.controllers;

import java.util.function.Supplier;

/** Executes a controller operation and translates failures to the presentation contract. */
public interface ControllerExecutor {
    <T> T execute(Supplier<T> operation);
}
