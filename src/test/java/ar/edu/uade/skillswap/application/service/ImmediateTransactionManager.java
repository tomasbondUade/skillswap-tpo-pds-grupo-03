package ar.edu.uade.skillswap.application.service;

import ar.edu.uade.skillswap.application.port.out.TransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class ImmediateTransactionManager implements TransactionManager {

    int operations;
    private final List<Runnable> afterCommitActions = new ArrayList<>();

    @Override
    public <T> T required(Supplier<T> work) {
        operations++;

        T result = work.get();

        // Simula el commit: las acciones se ejecutan solo si work termina bien.
        List<Runnable> actions = new ArrayList<>(afterCommitActions);
        afterCommitActions.clear();

        for (Runnable action : actions) {
            action.run();
        }

        return result;
    }

    @Override
    public void afterCommit(Runnable action) {
        afterCommitActions.add(action);
    }
}
