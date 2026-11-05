# Decisión de diseño: búsqueda filtrada, orden y paginación — issue #16

## Problema

La consigna (§4.3) pide buscar publicaciones por texto, categoría, modalidad, rango de precio, habilidad y reputación mínima; ordenar por relevancia, mejor reputación, menor precio o fecha más reciente, y paginar los listados. En `main`:

- no existía el filtro por habilidad y la reputación mínima se ignoraba en memoria;
- `BEST_RATING` ordenaba por cantidad de solicitudes;
- ningún orden tenía desempate, así que la paginación podía repetir u omitir publicaciones;
- las vistas de publicaciones no traían datos del autor, y la UI mostraba un nombre y una reputación inventados.

## Decisión

1. **La búsqueda sigue en `PublicationRepository.searchPublished`**, como el resto de las consultas del proyecto. Su comportamiento está definido en este documento (sección siguiente) y lo verifica el test de contrato `PublicationSearchContract`, que toda implementación (en memoria, JPA) debe extender.
2. **Toda `PublicationView` trae a su autor.** La base exige que toda publicación tenga un perfil de autor (FK `publications.author_user_id → profiles.user_id`), así que la vista lleva siempre un `AuthorView` (nombre, reputación y cantidad de calificaciones). La usan los seis casos de uso que devuelven publicaciones: `search` y `recommend` (#16/#17) y los de gestión de `PublicationUseCases` (#15). El dominio no cambia: `Publication` sigue referenciando al autor por id, como corresponde entre agregados.
3. **Los datos del autor se leen sin N+1.** En búsqueda y recomendaciones, `DiscoveryApplicationService` lee los perfiles de todos los autores de la página con `ProfileRepository.findByUserIds`, que es abstracto: cada implementación debe resolverlo en una lectura (en JPA, un `IN`). En gestión el autor es el propio usuario, así que `PublicationApplicationService` hace un único `findByUserId`. Crear un borrador sin perfil devuelve `NOT_FOUND`, igual que lo rechazaría la FK de la base.
4. **El filtro y el orden por autor se resuelven en el repositorio.** La búsqueda necesita la reputación y el estado del autor. En JPA es un join con la tabla de perfiles dentro de la consulta. En memoria, `InMemoryPublicationRepository` recibe el puerto `ProfileRepository` (no otra implementación concreta) y lee los perfiles de los autores en una sola llamada; es el equivalente en memoria de ese join y solo afecta al adaptador de demo y tests.
5. **Criterios autovalidados**, con la forma y la convención de `main`: `SearchQuery` mantiene sus campos (se agrega `skill`), recorta espacios, y el texto vacío o la categoría o habilidad nulas o en blanco significan "sin filtro". Las validaciones de rango de precio, reputación y página siguen en su constructor y lanzan `IllegalArgumentException`, como el resto de los comandos; `UiErrorMapper` las traduce a `VALIDACION`.
6. **Sin transacción en el service**, igual que las demás lecturas del proyecto (`listMyBookings`, `viewMyWallet`, etc.). La página y el total se resuelven en una sola llamada a `searchPublished`; la implementación JPA puede ejecutar esa consulta como de solo lectura.

## Comportamiento de `searchPublished`

| Regla | Comportamiento |
| --- | --- |
| Visibilidad | Solo publicaciones `PUBLISHED`. Solo de autores con perfil activo: se excluyen los dados de baja (decisión del equipo). Un autor sin perfil no puede existir por la FK. |
| Texto | Contenido en título, descripción, categoría o alguna etiqueta, sin distinguir mayúsculas ni acentos. Máximo 100 caracteres. |
| Categoría | Igualdad sin distinguir mayúsculas. |
| Modalidad | Igualdad. |
| Habilidad | Igualdad (sin mayúsculas ni acentos) con alguna etiqueta de la publicación. |
| Precio | Precio base dentro de `[mínimo, máximo]`, límites inclusivos. |
| Reputación mínima | Reputación del autor mayor o igual al mínimo, en `[0, 5]`. |
| Paginación | `page ≥ 0`, `1 ≤ size ≤ 100`; una página fuera de rango devuelve lista vacía con el total correcto. |

| Orden | Criterio | Desempates |
| --- | --- | --- |
| `RELEVANCE` | Solicitudes desc (popularidad), igual que en `main` | creación desc → id asc |
| `BEST_RATING` | Reputación del autor desc | calificaciones desc → creación desc → id asc |
| `LOWEST_PRICE` | Precio base asc | creación desc → id asc |
| `MOST_RECENT` | Creación desc | id asc |

## Alternativas descartadas

- **Strategy para el orden**: son cuatro variantes cerradas que fija la consigna y deben ejecutarse en el almacenamiento. Ordenar en Java después de paginar devolvería resultados incorrectos.
- **Que `Publication` contenga un `Profile`**: cruzaría dos agregados; cargar una publicación para reservar cupo arrastraría y bloquearía también el perfil. Entre agregados se referencia por id.
- **Un modelo aparte para descubrimiento (publicación + autor)**: duplicaba tipos. Como la FK garantiza que todo autor existe, una sola `PublicationView` con autor sirve para todos los casos.
- **Que el service filtre y ordene por autor**: obligaría a traer todas las publicaciones a memoria antes de paginar; en JPA se perdería la paginación en la base.
- **Una clase de almacenamiento compartida entre los repositorios en memoria**: evitaba la dependencia hacia `ProfileRepository` a costa de una clase que solo existía para eso.

## Supuestos

- "Habilidad" se compara con las etiquetas de la publicación, no con las habilidades del autor: se busca lo que enseña la oferta.
- El rango de precio usa el precio base; las promociones (Decorator) se aplican al cotizar.
- "Fecha más reciente" es la fecha de creación de la publicación.
- La búsqueda no requiere usuario autenticado porque no modifica información.
- Para que la exclusión de autores dados de baja sea coherente de punta a punta, `Booking.request` debería rechazar solicitudes a esos autores. Queda fuera de #16.

## Impacto en ramas abiertas

**PR #61 / #29 (persistencia JPA).** Al integrar #16:

| Cambio | Qué hay que adaptar |
| --- | --- |
| `SearchQuery` agrega `skill` | Los accessors existentes no cambian; el constructor recibe `skill` después de `modality`. Filtrar por `search.skill()`. |
| `ProfileRepository.findByUserIds` | `JpaProfileRepository` lo implementa con una única consulta `IN` y devuelve una lista vacía para una entrada vacía. |
| Contrato de `searchPublished` | `JpaPublicationRepository` implementa habilidad, texto en etiquetas, reputación mínima, autores activos y los cuatro órdenes con desempates estables. `JpaPublicationSearchContractTest` ejecuta el mismo contrato sobre MySQL. |
| Rendimiento | Unir con `profiles` (JOIN, garantizado por la FK) para filtrar y ordenar por reputación y excluir autores dados de baja; cargar etiquetas y franjas sin N+1; marcar la consulta como de solo lectura. Índices sugeridos: `(status, created_at)`, `(status, base_price)`, `(status, category)`, `publication_tag(tag)`, `profile(user_id)` único. |
| Acentos | La migración V4 aplica `utf8mb4_0900_ai_ci` a título, descripción, categoría y etiquetas para que MySQL coincida con el repositorio en memoria. |

**#15 (casos de uso de publicaciones).** `PublicationApplicationService` recibe `ProfileRepository` y sus vistas incluyen al autor; crear un borrador requiere que el usuario tenga perfil.

**#17 (recomendaciones).** `recommend` mantiene su firma (`List<PublicationView>`); ahora cada vista incluye al autor.

**#35 (trazabilidad).** `auditoria-patrones-y-grasp.md` e `informe-diseno.md` describen el corte `36b07b0` y no se modifican aquí; la fila de #16 se actualiza en el próximo corte completo.

**#30 (controllers de búsqueda).** La UI sigue recibiendo `List<Publicacion>` de la primera página. Exponer la paginación y los filtros nuevos (habilidad, precio mínimo, reputación mínima, página) en `BusquedaController` corresponde a ese issue.

## Evidencia

| Prueba | Cubre |
| --- | --- |
| `SearchQueryTest` | Valores por defecto, recorte de espacios y validación de texto, reputación, rango de precio y página. |
| `PublicationSearchContract`, `InMemoryPublicationSearchTest` y `JpaPublicationSearchContractTest` | Visibilidad, cada filtro, filtros en blanco, combinaciones, órdenes y desempates, paginación completa en memoria y MySQL. |
| `DiscoveryApplicationServiceTest` | Delegación sin modificar criterios, autor en una sola lectura de perfiles (búsqueda y recomendaciones), autor sin perfil como inconsistencia, validación, propagación de fallas, búsqueda sin autenticación. |
| `PublicationApplicationServiceTest` | Vistas de gestión con autor, borrador sin perfil rechazado, listado vacío sin perfil. |
| `InMemoryRepositoriesTest` | Lectura de perfiles en lote. |
| `UiModelMapperTest` / `WiredUiControllersTest` | Conversión de filtros de UI y nombre/reputación reales del autor en las tarjetas. |
