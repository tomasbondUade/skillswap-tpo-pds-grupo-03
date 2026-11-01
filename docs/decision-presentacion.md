# DTOs, mapeadores y errores de presentación — issue #27

## Problema y solución

La UI ya utilizaba los records de `Models`, pero sus conversiones estaban mezcladas
con la coordinación de casos de uso en `WiredUiControllers`. Las excepciones podían
llegar sin traducir a los eventos de Swing.

`UiModelMapper` convierte filtros de UI a consultas de aplicación y resultados de
aplicación a los modelos de la pantalla. Recibe el contexto de visualización
(nombre del proveedor, reputación, título y contraparte) sin consultar repositorios.
Se conservan los contratos y records existentes del Mock UI Kit.

Los DTOs contienen datos simples, enums y otros DTOs; nunca entidades de dominio
ni de JPA. Las listas de los modelos de UI se copian para evitar compartir estado
mutable. El dominio no depende de estos mapeadores.

## Contrato de errores

Las operaciones reales de `WiredUiControllers` se ejecutan mediante
`UiOperationExecutor`. Ante un fallo, lanzan `PresentationException`, con un
`ErrorPresentacion(codigo, mensaje)`. No se reemplazan errores por listas vacías,
ceros ni resultados que aparenten éxito.

| Origen | Código para la UI |
| --- | --- |
| Validación de entrada | `VALIDACION` |
| Falta de identidad | `NO_AUTENTICADO` |
| Recurso inexistente | `NO_ENCONTRADO` |
| Permisos insuficientes | `PERMISOS` |
| Operación incompatible con el estado | `ESTADO` |
| Conflicto de aplicación o bloqueo optimista | `CONFLICTO` |
| Persistencia JPA o JDBC | `PERSISTENCIA` |
| Error no previsto | `INESPERADO` |

`UiErrorMapper` traduce los errores de aplicación y de entrada. El reconocimiento
de excepciones JPA/JDBC permanece en infraestructura, incluido el bloqueo
optimista envuelto en un rollback. Los mensajes de persistencia y errores
inesperados son genéricos; la causa original queda disponible para diagnóstico.

`UiActionHandler` recibe el error en los eventos de Swing y muestra su mensaje.
La acción se interrumpe antes de mostrar confirmaciones de éxito. No cambia el
diseño de las pantallas ni incorpora endpoints HTTP.

## Responsabilidades y defensa

- El controller coordina el caso de uso.
- El mapper convierte datos; no agrega reglas de negocio ni accede a la base.
- La infraestructura reconoce fallos técnicos y los oculta a la pantalla.
- La vista muestra el resultado o el error.

Esto mantiene alta cohesión y bajo acoplamiento. La separación es compatible con
los ejemplos MVC/Clean Architecture de clase, con conversiones explícitas en Java.
No se incorpora un patrón de diseño adicional para justificar esta separación.

## Pruebas y límites

Las pruebas cubren conversiones, todos los enums, errores de aplicación,
persistencia, rollback con bloqueo optimista, acciones sin retorno, mensajes de
éxito interrumpidos y contratos sin entidades. Los tests existentes de controllers
siguen cubriendo búsqueda, publicación, contrataciones, saldo y notificaciones.

La rama parte de `main` y puede integrarse independientemente del PR #61. No
modifica sus migraciones, repositorios ni adaptadores. La demo sigue utilizando
su wiring en memoria y su identidad fija; conectar el wiring de persistencia y
reemplazar esa identidad corresponde a las tareas de integración respectivas.

Validación de esta implementación: `mvn clean test`, 98 pruebas, sin fallos ni
errores. El parche se aplica sin conflictos sobre el PR #61 (`391ace2`). Esa rama
ya tiene tres errores de compilación, reproducidos antes de aplicar este cambio:
el método `save(NotificationMessage)` pendiente en `InMemoryNotificationRepository`,
la firma del constructor de reservas en `SkillSwapApplicationWiring` y la referencia
a `ESCROW_WALLET_ID` en ese wiring. Deben corregirse en la integración del PR #61.
