# Guion de demo: SkillSwap

## Objetivo

Mostrar la experiencia Swing conectada a casos de uso de Application para perfiles, publicaciones, búsqueda, contrataciones, billetera, calificaciones y notificaciones. La app inicia como `Mateo Silva` y permite seleccionar a Ana o Lucía desde **Cambiar usuario demo**; usa repositorios en memoria.

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
4. **Gestionar una contratación.** Las acciones de transición aparecen según el participante activo. Usar **Cambiar usuario demo** para seleccionar al proveedor de la solicitud y aceptarla; volver a Mateo para pagar y alternar al participante autorizado para iniciar/completar. Comprobar que el saludo, nombre, avatar y rol del pie lateral corresponden a cada identidad y que el saldo del encabezado coincide con **SkillCoins**. Después de pagar, completar, cancelar o acreditar, volver a comparar ambos saldos.
5. **Publicar.** Abrir **Mis publicaciones**, crear un borrador con título, descripción y categoría, y presionar **Publicar**. El controller agrega una franja demo válida y la publicación pasa a estar disponible en búsqueda.
6. **Billetera y notificaciones.** **SkillCoins** consulta saldo y ledger; **Acreditar demo** registra un movimiento en el agregado Wallet. **Notificaciones** permite marcar una o todas como leídas.
7. **Calificar y mostrar el perfil.** Como participante de una contratación completada, usar **Calificar**, ingresar puntuación y comentario y comprobar la reputación del destinatario. Verificar también el rechazo de una calificación duplicada. La sesión de demo no reemplaza autenticación real.

Este guion es una lista de verificaciones pendientes de ejecución visual completa; no constituye evidencia de aceptación del recorrido.

## Límites de esta demo

La demo integra los servicios y reglas disponibles, pero todavía no es una entrega de producción:

- El formulario de solicitud permite elegir horario e ingresar objetivo y mensaje. Verificar esos datos al recorrer la contratación.
- El pago debita SkillCoins y los mueve a una wallet de custodia; la finalización libera el saldo al proveedor y la cancelación pagada aplica `StandardRefundPolicy`, devuelve la parte correspondiente al cliente y liquida el remanente al proveedor. Son saldos ficticios y no existe pasarela de pago.
- El selector solo admite las tres identidades de demo (`user-mateo`, `mentor-ana`, `mentor-lu`); no es un sistema de autenticación.
- `SynchronousTransactionManager` no aporta rollback ni bloqueo; el estado se pierde al reiniciar.
- El botón de calificación está disponible en contrataciones completadas; el recorrido visual completo y la actualización de reputación todavía requieren comprobación manual.

La integración con el PR #61 y la validación de persistencia JPA/MySQL, migraciones y garantías de rollback/concurrencia siguen pendientes. Los eventos Observer y la bandeja in-app están integrados en memoria; no hay proveedor externo real. Consultar `issue33-integracion-y-validacion.md` para el estado de pruebas y cobertura.
