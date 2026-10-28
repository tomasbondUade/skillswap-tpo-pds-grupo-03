# Integración de Adapter y Simple Factory

## Problema y resultado

Las clases de identidad, canales de notificación y su factory estaban presentes,
pero el wiring de la aplicación seguía utilizando los componentes anteriores.
Además, los productores y los observadores usaban nombres de eventos distintos.

`SkillSwapApplicationWiring` ahora construye `IdentityProviderAdapter` y una
`NotificationFactory` compartida por los observadores de reservas y calificaciones.
Los servicios siguen dependiendo de puertos de aplicación. No conocen los
clientes mock de identidad, email o push.

## Recorrido de los patrones

- **Identidad:** el servicio pide `CurrentUserProvider.currentUserId()`; el adapter
  obtiene `IdentityResponse` del cliente mock y extrae `subject`.
- **Reservas:** `NotificationEventObserver` recibe el evento, busca las preferencias
  del destinatario y pide un adapter a la factory por cada canal elegido.
- **Calificaciones:** `RatingNotificationEventPublisher` procesa `rating.submitted`
  y lee el destinatario de `subjectUserId`, igual que el evento emitido por el servicio.
- **Creación:** `NotificationFactory.create(channel)` devuelve un
  `NotificationGateway` concreto mediante un `switch`.
- **Envío:** Email y Push convierten `NotificationMessage` a las operaciones de sus
  clientes mock; InApp delega en `NotificationRepository.save`.

Adapter traduce interfaces incompatibles mediante composición. Simple Factory
centraliza la selección y construcción de la implementación. La factory recibe sus
dependencias por constructor; no necesita un método estático para ser Simple Factory.

## Contratos de eventos

Se conserva la convención de `main`: `booking.requested`, `booking.accepted`,
`booking.rejected`, `booking.paid`, `booking.cancelled`, `booking.started`,
`booking.completed`, `booking.reschedule-proposed`, `booking.reschedule-accepted`,
`booking.rescheduled` y `rating.submitted`.

La aceptación de una reprogramación emite `booking.rescheduled` cuando se completa
el acuerdo y `booking.reschedule-accepted` cuando todavía falta una respuesta.

## Reparaciones de integración

- Implementar el método faltante `save(NotificationMessage)` del repositorio en memoria.
- Corregir el orden de dependencias del constructor de reservas.
- Quitar del wiring la referencia a una billetera de custodia que no existe en el
  servicio de esta rama: se conserva su transferencia entre comprador y proveedor.
- Reparar una aserción de tests que referenciaba una lista inexistente.
- Conservar el mensaje del cliente al crear y devolver una contratación.
- Validar permisos y estado antes de reservar un cupo, para que una aceptación
  repetida no consuma otro lugar en la demo en memoria.

## Demostración y pruebas

El usuario predeterminado de la demo sigue siendo `user-mateo`, obtenido mediante
el adapter. El perfil de `mentor-ana` se inicializa con Email, Push e InApp. Al
solicitar la publicación “Entrevistas técnicas con feedback real”, los dos clientes
mock escriben sus envíos en consola y se guarda la notificación interna para Ana.
No se envían mensajes a proveedores reales.

Las pruebas recorren la solicitud y el ciclo aceptar → pagar → iniciar → completar
→ calificar usando el wiring real. Verifican destinatarios, canales, notificaciones
sin duplicados, lectura de bandeja, reputación, mensaje del cliente y cambio de
identidad mediante el cliente mock. También cubren una aceptación repetida con
cupo todavía disponible.

Validación: `mvn clean test`, 88 pruebas sin fallos ni errores. La combinación con
la rama de la issue #27 pasa 113 pruebas. También se comprobó el inicio de la
pantalla con el modo de captura de la aplicación, sin requerir una base MySQL.

La demo continúa usando repositorios en memoria. Esta corrección conecta los
patrones al recorrido ejecutable; no agrega una conexión MySQL ni autenticación real.
