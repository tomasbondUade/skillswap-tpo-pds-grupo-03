package ar.edu.uade.skillswap.infrastructure.persistence;

import ar.edu.uade.skillswap.application.port.out.TransactionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Implementación JPA del puerto de transacciones de la aplicación.
 *
 * Comparte un EntityManager entre las operaciones ejecutadas dentro
 * de una misma transacción y garantiza commit o rollback.
 */
public final class JpaTransactionManager implements TransactionManager {

    private final EntityManagerFactory entityManagerFactory;

    private final ThreadLocal<EntityManager> currentEntityManager =
            new ThreadLocal<>();

    private final ThreadLocal<List<Runnable>> afterCommitActions =
            new ThreadLocal<>();

    public JpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory =
                Objects.requireNonNull(
                        entityManagerFactory,
                        "Falta el EntityManagerFactory"
                );
    }

    /**
     * Devuelve el EntityManager asociado a la transacción actual.
     */
    public EntityManager currentEntityManager() {
        EntityManager entityManager = currentEntityManager.get();

        if (entityManager == null || !entityManager.isOpen()) {
            throw new IllegalStateException(
                    "No hay una transacción activa en este hilo"
            );
        }

        return entityManager;
    }

    /**
     * Registra una acción para ejecutarla después del commit exterior.
     */
    @Override
    public void afterCommit(Runnable action) {
        Objects.requireNonNull(action, "Falta la acción");

        List<Runnable> actions = afterCommitActions.get();

        if (actions == null) {
            throw new IllegalStateException(
                    "No hay una transacción activa para registrar la acción"
            );
        }

        actions.add(action);
    }

    @Override
    public <T> T required(Supplier<T> work) {
        Objects.requireNonNull(work, "Falta la operación");

        // Si ya existe una transacción, reutilizamos el contexto exterior.
        EntityManager existing = currentEntityManager.get();

        if (existing != null) {
            if (!existing.isOpen()
                    || !existing.getTransaction().isActive()) {
                throw new IllegalStateException(
                        "El contexto de transacción existente no está activo"
                );
            }

            return work.get();
        }

        EntityManager entityManager =
                entityManagerFactory.createEntityManager();

        EntityTransaction transaction = entityManager.getTransaction();

        List<Runnable> actions = new ArrayList<>();

        currentEntityManager.set(entityManager);
        afterCommitActions.set(actions);

        T result;

        try {
            transaction.begin();

            result = work.get();

            transaction.commit();

        } catch (RuntimeException | Error exception) {
            if (transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }

            throw exception;

        } finally {
            // Se limpia el contexto antes de ejecutar las acciones externas.
            currentEntityManager.remove();
            afterCommitActions.remove();

            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }

        // Solo llegamos acá si la operación y el commit terminaron bien.
        // Las acciones se ejecutan fuera de la transacción original.
        for (Runnable action : actions) {
            action.run();
        }

        return result;
    }
}
