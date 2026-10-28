package ar.edu.uade.skillswap.infrastructure.persistence;

import jakarta.persistence.EntityManager;

import java.util.Objects;
import java.util.function.Function;

/**
 * Soporte común para los repositorios JPA.
 */
public abstract class JpaRepositorySupport {

    private final JpaTransactionManager transactions;

    protected JpaRepositorySupport(JpaTransactionManager transactions) {
        this.transactions = Objects.requireNonNull(
                transactions, "Falta el administrador de transacciones");
    }

    /**
     * Accede al EntityManager de la transacción actual.
     * Se utiliza para operaciones de escritura.
     */
    protected final EntityManager entityManager() {
        return transactions.currentEntityManager();
    }

    /**
     * Ejecuta una consulta dentro de una transacción.
     *
     * Si ya existe una transacción, reutiliza su contexto.
     * Si no existe, JpaTransactionManager inicia una.
     */
    protected final <T> T query(Function<EntityManager, T> operation) {
        Objects.requireNonNull(operation, "Falta la consulta");

        return transactions.required(
                () -> operation.apply(entityManager()));
    }
}
