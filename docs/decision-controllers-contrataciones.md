# Issue #31: Controllers del ciclo de contratación

`ContratacionUiController` valida el `userId` recibido contra `CurrentUserProvider`, valida campos obligatorios y delega en `BookingUseCases` las operaciones de solicitud, aceptación, rechazo, pago, cancelación, reprogramación, inicio y finalización.

El controller no accede a JPA ni implementa reglas de cupos, saldo, estado o participantes: corresponden a Application y Domain. `ControllerExecutor` reutiliza el contrato de errores de presentación de la #27. `WiredUiControllers` delega `contrataciones()` al controller específico; `avanzar()` y `puedeAvanzar()` se mantienen para el Mock UI Kit.

El PR #31 depende del #28, que depende del #27. Las acciones visuales nuevas corresponden a la integración final de la #33. Las pruebas unitarias cubren delegación, validación, identidad, estados y errores.
