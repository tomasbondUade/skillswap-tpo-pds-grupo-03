# Issue #6 — checklist de validación manual

Este checklist prepara la verificación visual del recorrido principal de SkillSwap. Está pensado para ejecutarse sobre una rama de integración que incluya los controllers y la pantalla de demo de los PR #63–#65. **No indica que el recorrido ya se haya ejecutado ni que esos PR estén integrados en `main`.**

## Alcance y límites

- La demo Swing usa repositorios en memoria. Las reservas, saldos y calificaciones se reinician al cerrar la aplicación.
- Los SkillCoins son ficticios; esta prueba no valida cobros reales, MySQL, Flyway ni concurrencia de persistencia. Esas validaciones son independientes.
- El entorno donde se preparó esta checklist no tenía una sesión gráfica disponible. La ventana inicial se pudo renderizar a una captura, pero el recorrido con interacción queda pendiente.
- Hallazgo a verificar en el PR #65: al cambiar de usuario, la vista sigue mostrando el saludo, el pie lateral y el saldo del encabezado de Mateo. La prueba de cambio de identidad debe quedar en verde cuando esos indicadores se refresquen.

## Preparación

1. Usar la rama integrada más reciente que contenga los PR de presentación y el flujo de contratación.
2. Tener JDK 17 o superior y Maven.
3. Ejecutar desde la raíz del repositorio:

   ```bash
   mvn clean test
   mvn exec:java
   ```

4. Confirmar que abre **SkillSwap · Mock UI Kit**. La demo inicializa los perfiles `user-mateo`, `mentor-ana` y `mentor-lu`.
5. Registrar el SHA probado y capturas de cada resultado. No incluir datos personales en las capturas.

## Recorrido principal: publicar → solicitar → aceptar → pagar → completar → calificar

| Paso | Acción | Resultado esperado | Estado |
| --- | --- | --- | --- |
| 1. Identidad inicial | Iniciar como `user-mateo` y abrir Inicio y Mi perfil. | Se muestran los datos del usuario activo y su saldo. | ☐ Pendiente |
| 2. Crear oferta | Abrir **Mis publicaciones** → **Nueva publicación**. Crear una oferta de prueba con título único y descripción; confirmar. | Se crea un borrador visible en **Mis publicaciones**. | ☐ Pendiente |
| 3. Publicar | Pulsar **Publicar** sobre el borrador. | Cambia a estado publicado y aparece en **Explorar**. | ☐ Pendiente |
| 4. Cambiar solicitante | Usar **Cambiar usuario demo** y seleccionar `mentor-ana`; abrir **Mi perfil** y revisar nombre, saldo y encabezado. | El perfil y los indicadores visuales corresponden a Ana. Si el saludo, pie lateral o saldo del encabezado aún muestran Mateo, registrar fallo visual. | ☐ Pendiente |
| 5. Preparar saldo | Abrir **SkillCoins** y acreditar 500 SkillCoins de demo. | El saldo de Ana queda en 500. | ☐ Pendiente |
| 6. Solicitar | En **Explorar**, abrir la oferta creada. Seleccionar una franja disponible, escribir un objetivo y un mensaje distintos, y solicitar la mentoría. | Se confirma la solicitud y aparece en Contrataciones como pendiente, con objetivo, mensaje y franja correspondientes. | ☐ Pendiente |
| 7. Aceptar | Cambiar a `user-mateo`; abrir **Contrataciones** y avanzar el estado de la solicitud. | Mateo, como proveedor, puede aceptar; el estado pasa a aceptada. | ☐ Pendiente |
| 8. Pagar | Cambiar a `mentor-ana`; avanzar el estado. | Ana, como solicitante, paga; el estado pasa a confirmada y su saldo disminuye por el precio de la oferta. | ☐ Pendiente |
| 9. Completar | Como uno de los participantes, avanzar dos veces: iniciar y luego completar la sesión. | Los estados pasan por en curso y completada; no aparece el botón de avance al finalizar. | ☐ Pendiente |
| 10. Calificar | En la contratación completada, pulsar **Calificar**, elegir un puntaje y escribir un comentario. | La calificación se confirma y queda asociada a esa contratación. | ☐ Pendiente |
| 11. Notificación | Cambiar a `user-mateo` y abrir **Notificaciones**. | Mateo recibe la notificación de la nueva calificación conforme a sus preferencias. | ☐ Pendiente |

## Comprobaciones negativas

| Caso | Acción | Resultado esperado | Estado |
| --- | --- | --- | --- |
| Objetivo vacío | Abrir el detalle de una oferta, dejar vacío el objetivo y solicitar. | La UI informa que falta el objetivo y no crea una contratación. | ☐ Pendiente |
| Franja no disponible | Intentar solicitar sin una franja seleccionable. | El botón queda deshabilitado o la solicitud se rechaza con un mensaje entendible. | ☐ Pendiente |
| Usuario ajeno | Cambiar a `mentor-lu` mientras la contratación está en curso. | No puede verla ni cambiar su estado si no participa de ella. | ☐ Pendiente |
| Calificación duplicada | Intentar calificar nuevamente la misma contratación y como el mismo usuario. | El caso de uso rechaza la segunda calificación y la UI muestra el error sin duplicar el registro. | ☐ Pendiente |

## Registro de ejecución

Completar durante la prueba manual:

- Fecha y entorno: pendiente.
- SHA probado: pendiente.
- Resultado: pendiente.
- Casos fallidos y evidencia: pendiente.
- Observaciones para el PR: pendiente.

No marcar casos como aprobados a partir de pruebas unitarias o de la captura estática; cada casilla requiere interacción real con la aplicación.
