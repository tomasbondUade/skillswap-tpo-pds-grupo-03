# Decisión de diseño: organizaciones y publicaciones de tipo desafío — issue #38

## Problema

La consigna (§4.2) dice: "Un profesional podrá publicar una oferta de mentoría o servicio. Una organización podrá publicar un desafío." En `main` toda cuenta era un perfil profesional y toda publicación era igual, así que no había forma de representar organizaciones ni desafíos.

Qué define la consigna para cada tipo:

| Aspecto | Mentoría o servicio | Desafío |
| --- | --- | --- |
| Quién la publica | Profesional | Organización |
| Datos | Los de "toda publicación": título, descripción, categoría, modalidad, duración, cupo, precio base y etiquetas | Los mismos |
| Estados y transiciones | Borrador, publicada, pausada, cerrada, cancelada | Los mismos |
| Contratación, pago y calificación (§4.4, §5) | Flujo genérico sobre "publicación" | El mismo |
| Casos de aceptación (§15) | Casos 1–3 | Ninguno propio |

La única regla que distingue a los tipos es quién puede crear cada uno.

## Decisión

1. **Tipo de actor en el perfil.** `Profile` tiene un `ActorType` (`PROFESSIONAL` u `ORGANIZATION`) que se fija al registrarse y no cambia. La organización es un perfil más, no un agregado aparte, porque comparte todo lo demás: identidad mock, nombre, notificaciones, billetera, reputación y baja lógica.
2. **Tres tipos de publicación.** `PublicationType` tiene `MENTORSHIP`, `SERVICE` y `CHALLENGE`. Mentoría y servicio se separan para que el profesional elija cómo clasifica su publicación, aunque se comportan igual: comparten datos, estados y flujo.
3. **El tipo de actor define las opciones, no un único tipo.** `ActorType` concentra la regla: el profesional puede crear mentorías y servicios, y la organización, desafíos (`publicationTypes()`, `canPublish(tipo)`). Agregar un tipo de publicación, o habilitarlo para otro actor, solo cambia ese enum; no hay condicionales por tipo en el resto del código.
4. **El autor elige el tipo y el dominio lo valida.** El tipo es obligatorio: no hay valores por defecto ni para el tipo de actor ni para el de publicación. `Publication.draft(...)` y `Publication.builder(...)` reciben el tipo de actor del autor y el tipo elegido, y rechazan con `SecurityException` (`FORBIDDEN` en Application, `PERMISOS` en la UI) un tipo que el actor no puede crear. La UI ofrece solo las opciones válidas, pero ese filtro no reemplaza la validación. `Publication` no conoce a `Profile`: solo recibe el dato que necesita, como ya recibe el id del autor.
5. **Opciones para la UI.** `PublicationUseCases.allowedPublicationTypes()` devuelve los tipos que puede crear el usuario actual, en orden, para armar el selector del formulario.
6. **Mismos permisos y transiciones para todos los tipos.** El autor es el único que edita, publica, pausa, cierra y cancela, y las transiciones inválidas se rechazan igual (State). Un autor no puede solicitar su propia publicación.

## Cambios por capa

| Capa | Cambio |
| --- | --- |
| Domain | `ActorType` con sus tipos permitidos y `PublicationType`. `Profile.actorType()`; `Profile.register(userId, actorType, ...)` exige el tipo de actor. `Publication.type()`; `draft` y `builder` reciben el tipo de actor del autor y el tipo elegido. |
| Application | `RegisterCommand` y `CreateDraftCommand` exigen el tipo (sin tipo → `VALIDATION`). `allowedPublicationTypes()` en `PublicationUseCases`. `ProfileView` y `PublicationView` exponen los tipos. |
| Infrastructure | Migración V5: columnas `profiles.actor_type` y `publications.publication_type`, con `CHECK` sobre los valores válidos. Las filas existentes quedan como profesionales y mentorías, y el valor por defecto se elimina después para que todo alta indique el tipo. Mapeo JPA. El seed indica el tipo de cada publicación (dos mentorías y un servicio). |
| Presentation | `TipoPublicacion` (Mentoría, Servicio, Desafío) en `Models` y en `Publicacion`. `PublicacionController` suma `tiposPublicacion(userId)` y `crearBorrador` recibe el tipo elegido. El formulario "Nueva publicación" ofrece solo los tipos del usuario y "Mis publicaciones" muestra el tipo. El mock implementa lo mismo para un profesional. |

## Alternativas descartadas

- **Derivar el tipo de publicación del tipo de actor (un tipo por actor).** Fue la primera versión. Es más simple, pero obliga a cambiar la regla cuando un actor pueda crear más de un tipo, y no permite que la UI ofrezca opciones.
- **Un solo tipo "oferta" para mentoría y servicio.** Es la lectura mínima de la consigna, que no les da reglas distintas. Se separaron para que el profesional elija la clasificación; si en el futuro difieren en alguna regla, ya son tipos distintos.
- **Un agregado `Organization` separado de `Profile`.** Duplicaría nombre, notificaciones, reputación y baja lógica, y obligaría a que el autor de una publicación pudiera estar en dos tablas distintas, sin ninguna regla que lo justifique.
- **Subclases de `Profile` o de `Publication` (herencia).** No hay comportamiento distinto que sobrescribir; solo cambia qué tipos se pueden crear. Un enum con ese dato es más simple y en JPA evita una estrategia de herencia.
- **Campos propios del desafío** (fecha límite, entregable, premio). La consigna no define ninguno; agregarlos sería inventar reglas de negocio.
- **Invertir el pago en los desafíos** (que la organización pague a quien lo resuelve). Ningún texto de la consigna lo indica. Con el flujo genérico, la organización acepta las solicitudes y cobra, como una inscripción con cupo.

## Supuestos

- Una organización también puede contratar mentorías o servicios como cliente; la consigna no lo prohíbe.
- Calificaciones y reputación funcionan igual para organizaciones.
- La búsqueda y las recomendaciones incluyen todos los tipos; la consigna no pide filtrar por tipo.

## Impacto en ramas abiertas

| Rama | Qué hay que adaptar al integrar |
| --- | --- |
| PR #63 (#28) | `PublicacionUiController` debe implementar `tiposPublicacion(userId)` y pasar el tipo en `crearBorrador` (y en `DatosBorrador` si se usa esa variante). `UiModelMapper` debe pasar el tipo al crear `RegisterCommand` y `CreateDraftCommand`. `PublicationSpy` debe sumar `allowedPublicationTypes()`. Sus tests crean `ProfileView`, `PublicationView` y `Publicacion`, que ahora reciben el tipo. |
| PR #65 (#33, WIP) | `Publication.builder(id)` pasa a `builder(id, actorType, type)` y `Profile.register` exige el tipo de actor (seed y `MySqlPersistenceIntegrationTest`). Su mock suma `tiposPublicacion` y el tipo en `crearBorrador` y en `Publicacion`. Los cambios de `SkillSwapView` no tocan las mismas líneas. |
| #6 (presentación) | Mostrar el tipo en las tarjetas de Explorar y el detalle, y el alta de organizaciones desde la UI. |
| #26 (datos demo) | Agregar al seed una organización con un desafío publicado. |

## Evidencia

| Prueba | Cubre |
| --- | --- |
| `ActorPublicationTypeTest` | Tipos permitidos de cada actor; todo tipo tiene quien lo cree; cada combinación de actor y tipo se acepta o se rechaza según la regla (draft y Builder); registro con tipo; tipos obligatorios; ciclo de vida, permisos del autor y transiciones inválidas para cada tipo; una organización no puede solicitar su propio desafío. |
| `ProfileApplicationServiceTest` | Registro de una organización, tipo en la vista y comando sin tipo rechazado. |
| `PublicationApplicationServiceTest` | Tipo elegido permitido y rechazado (`FORBIDDEN`), tipo obligatorio, opciones del usuario actual y usuario sin perfil. |
| `ActorAndPublicationTypeMapperTest` | Ida y vuelta de ambos tipos entre dominio y entidades JPA. |
| `WiredUiControllersTest` | Opciones del usuario de la demo, borrador del tipo elegido, sin tipo como `VALIDACION`, tipo en "Mis publicaciones" y tipo no permitido como `PERMISOS` sin crear nada. |
| `MockSkillSwapControllersTest` / `UiModelMapperTest` | El mock ofrece y conserva el tipo; conversión de los tres tipos entre Application y la UI. |
| `MySqlPersistenceIntegrationTest` | Organización y desafío en MySQL real, incluida la búsqueda; migración desde V4 que completa filas existentes y rechaza filas sin tipo o con un tipo inválido. |
