# Controllers de perfil y publicaciones — issue #28

## Resultado

`PerfilUiController` y `PublicacionUiController` implementan las interfaces Java de
`UiControllers`. Reciben puertos de aplicación y la identidad activa por constructor.
`WiredUiControllers` conserva la fachada que consume Swing y delega estas operaciones
en los nuevos controllers.

| Operación de UI | Puerto de aplicación |
| --- | --- |
| `crearPerfil(userId, NuevoPerfil)` | `ProfileUseCases.register` |
| `verPerfil(userId)` | `ProfileUseCases.viewMyProfile` |
| `crearBorrador(userId, DatosBorrador)` | `PublicationUseCases.createDraft` |
| `crearBorrador` con los parámetros del kit | `PublicationUseCases.createDraft` |
| `editarBorrador(userId, publicacionId, EdicionBorrador)` | `PublicationUseCases.editDraft` |
| `cambiarEstado(userId, publicacionId, estado)` | `PublicationUseCases.changeStatus` |
| `listarPropias(userId)` | `PublicationUseCases.listMyPublications` |

La firma original de creación sigue disponible para la pantalla entregada: utiliza
el cupo demo de seis y una franja para el día siguiente. El nuevo DTO permite
especificar cupos, etiquetas y disponibilidad. La edición conserva los campos que
admite el caso de uso existente; no altera precio o disponibilidad por su cuenta.
El mock del kit también implementa los nuevos contratos.

## Identidad, permisos y errores

`ControllerUser` verifica en cada llamada que exista identidad activa, que se haya
recibido un `userId` y que ambos identificadores coincidan. Un parámetro recibido de
la UI no puede reemplazar la identidad autenticada. El punto de composición entrega
el mismo `CurrentUserProvider` a los servicios y a los controllers.

El caso de uso sigue verificando la propiedad del recurso y el dominio controla
qué se puede editar o qué transición está permitida. Los controllers no reproducen
esas reglas. Las consultas de perfil y publicaciones propias corresponden al
usuario activo.

Se reutiliza `PresentationException` de la #27 para validaciones, identidad ausente,
permisos, recurso inexistente, conflicto y estado inválido. `ControllerExecutor`
permite reutilizar el traductor de infraestructura sin que los controllers dependan
de JPA, repositorios concretos ni clases de infraestructura.

## DTOs y presentación

`NuevoPerfil`, `DatosBorrador`, `EdicionBorrador` y `Disponibilidad` contienen datos
simples, enums y colecciones copiadas. `UiModelMapper` los convierte a comandos y
transforma las respuestas del servicio. No se exponen entidades de dominio o JPA.

El perfil usa un rol genérico y cero proyectos porque ese contador no existe en
`ProfileView`. Para las publicaciones propias, `PublicationView` aporta el id del
autor; la búsqueda y el enriquecimiento completo de tarjetas conservan su recorrido
existente. Esta issue no incorpora nuevos campos al dominio.

## Pruebas y revisión

Los tests con puertos de prueba verifican comandos completos, resultados del caso
de uso, delegación única y rechazo antes de invocar el servicio. Los tests con casos
de uso reales y repositorios en memoria cubren crear/consultar perfil, duplicados,
editar/publicar/pausar, propiedad del recurso, entradas inválidas y recursos ausentes.
También se prueba la fachada de Swing y la compatibilidad del mock.

Validación del 05/11/2026: `mvn -Pmysql-it test`, 234 pruebas, sin fallos, errores
ni omisiones, contra MySQL 8.4.13. Se conservan las pruebas de búsqueda paginada,
identidad y permisos del contrato actual. La revisión adapta los controllers y
sus fixtures a `PublicationView.author`, reutiliza `BusquedaUiController` para
búsqueda/recomendaciones y evita inventar nombres o reputaciones en presentación.

La rama se actualizó sobre `main` después de la integración del PR #62, y el PR #63
ahora apunta a `main`. El controller recibe el proveedor de identidad del wiring;
el PR #61 ya está integrado en la base con su adapter de identidad y el modo JPA.

Se mantiene la interfaz gráfica entregada. La incorporación de controles visuales
para las nuevas operaciones pertenece al ensamblado final de la UI de la #33.
