# Guion de demo: SkillSwap Mock UI Kit

## Objetivo

Mostrar la experiencia de navegación y el flujo de publicaciones y contrataciones disponible en la versión actual. La app usa un usuario de demostración, `Mateo Silva`, y datos en memoria.

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
3. **Solicitar.** Presionar **Solicitar mentoría**. La solicitud aparece en **Contrataciones** con estado `PENDIENTE`.
4. **Avanzar la contratación.** En **Contrataciones**, presionar **Avanzar estado** para recorrer `PENDIENTE → ACEPTADA → CONFIRMADA → EN_CURSO → COMPLETADA`. La acción avanza un estado por vez.
5. **Mostrar publicaciones propias.** Abrir **Mis publicaciones**, crear un borrador con título, descripción y categoría, y presionar **Publicar** para cambiar su estado en el mock.
6. **Mostrar billetera y notificaciones.** Abrir **SkillCoins** para ver saldo y movimientos de ejemplo; se puede usar **Acreditar demo**. En **Notificaciones**, marcar una notificación o todas como leídas.
7. **Mostrar el perfil.** La pantalla de perfil presenta habilidades y reputación de ejemplo.

## Límites de esta demo

Esta versión verifica la navegación y las respuestas del Mock UI Kit, no un recorrido integrado de extremo a extremo:

- Las solicitudes de la interfaz usan un objetivo fijo y no permiten elegir una franja ni escribir un mensaje propio.
- **Avanzar estado** simula aceptación, confirmación de pago, inicio y finalización con una acción genérica. No debita SkillCoins ni ejecuta una transacción real.
- La pantalla no permite crear una calificación; muestra una reputación y reseñas precargadas.
- Las publicaciones del mock no editan disponibilidad y se pierden al reiniciar la aplicación.

Al defender el TP, presentar estos puntos como límites del scaffold actual. El recorrido final esperado del sistema integrado es **publicar → solicitar → aceptar → pagar → completar → calificar**, coordinado por los casos de uso y las capas de persistencia y presentación.
