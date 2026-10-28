# Guion de demo: SkillSwap

## Objetivo

Mostrar la experiencia Swing conectada a casos de uso de Application para perfiles, publicaciones, búsqueda, contrataciones, billetera y notificaciones. La app usa un usuario de demostración, `Mateo Silva`, y repositorios en memoria.

## Preparación

Desde la raíz del repositorio, ejecutar:

```bash
mvn clean test
mvn exec:java
```

La primera orden valida compilación y pruebas; la segunda abre la ventana Swing. No se requiere una base de datos para esta demo.

## Recorrido disponible

1. **Inicio y recomendaciones.** Mostrar el inicio, las recomendaciones y las categorías.
2. **Explorar una mentoría.** Abrir **Explorar**, filtrar por texto/categoría/orden y entrar en el detalle de una publicación.
3. **Solicitar.** Presionar **Solicitar mentoría**. La solicitud real de aplicación aparece en **Contrataciones** como `PENDIENTE`; se puede cancelar como comprador.
4. **Gestionar una contratación.** Las acciones de transición aparecen según el participante autenticado. Mateo no puede aceptar solicitudes dirigidas a otros mentores; la demo no ofrece cambio de sesión para esos proveedores.
5. **Publicar.** Abrir **Mis publicaciones**, crear un borrador con título, descripción y categoría, y presionar **Publicar**. El controller agrega una franja demo válida y la publicación pasa a estar disponible en búsqueda.
6. **Billetera y notificaciones.** **SkillCoins** consulta saldo y ledger; **Acreditar demo** registra un movimiento en el agregado Wallet. **Notificaciones** permite marcar una o todas como leídas.
7. **Mostrar el perfil.** La pantalla de perfil presenta habilidades y reputación de ejemplo.

## Límites de esta demo

La demo integra los servicios y reglas disponibles, pero todavía no es una entrega de producción:

- Las solicitudes usan un objetivo fijo y el primer horario disponible; no permiten elegir franja ni escribir un mensaje propio.
- El pago debita SkillCoins y los mueve a una wallet de custodia; la finalización libera el saldo al proveedor y la cancelación pagada aplica `StandardRefundPolicy`, devuelve la parte correspondiente al cliente y liquida el remanente al proveedor. Son saldos ficticios y no existe pasarela de pago.
- El usuario autenticado es fijo (`user-mateo`), por lo que la interfaz no puede operar como proveedor en contrataciones de otros usuarios.
- `SynchronousTransactionManager` no aporta rollback ni bloqueo; el estado se pierde al reiniciar.
- La pantalla no permite crear una calificación; la reputación y las reseñas siguen siendo de demostración.

La persistencia JPA/MySQL, migraciones y garantías de rollback/concurrencia de producción siguen pendientes. Los eventos Observer y la bandeja in-app están integrados en memoria; no hay proveedor externo ni cambio de sesión para recorrer el flujo completo con dos usuarios desde la UI.
