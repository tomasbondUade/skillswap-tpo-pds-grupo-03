# Decisión de diseño: controllers de búsqueda y recomendación — issue #30

Depende de #16 (PR #70): usa `SearchQuery` con `skill` y `PublicationView` con `AuthorView`.

## Problema

La consigna (§4.3 y §9) pide paginar todos los listados de los controllers de consulta. Antes de este cambio:

- `BusquedaController.buscar` devolvía `List<Publicacion>`, siempre la primera página de 20;
- `FiltrosBusqueda` no tenía habilidad, precio mínimo, reputación mínima ni página;
- la pantalla Explorar no permitía navegar páginas ni usar esos filtros;
- una etiqueta de orden desconocida se convertía en "Relevancia" sin avisar;
- la lógica de búsqueda y recomendación vivía dentro de `WiredUiControllers`, junto con todos los demás controllers.

## Decisión

1. **Un controller dedicado en `controllers`**, con el patrón de #28 (PR #63): `BusquedaUiController` implementa `BusquedaController` (`buscar`, `recomendar`, `categorias`). Recibe el puerto `DiscoveryUseCases`, la identidad activa y un `ControllerExecutor`, y solo convierte datos y delega. `WiredUiControllers` lo arma y le delega esas operaciones. El contrato `BusquedaController` mantiene sus tres operaciones; solo cambia el tipo que devuelve `buscar`.
2. **Resultado paginado:** `buscar` devuelve `Models.PaginaPublicaciones` (items, página, tamaño, total, total de páginas, `hayAnterior()`, `haySiguiente()`). No contiene entidades ni modelos de Application.
3. **Filtros completos:** `FiltrosBusqueda` agrega habilidad, precio mínimo, reputación mínima, página y tamaño. Se conserva la firma anterior de cinco parámetros, que devuelve la primera página de 20, para no romper los llamados existentes.
4. **Orden estricto:** las etiquetas válidas son "Relevancia" (o vacío), "Mejor reputación", "Menor precio" y "Más recientes". Cualquier otra da `VALIDACION`.
5. **Recomendaciones sin paginar:** `recomendar` devuelve las 3 mejores publicaciones según la estrategia (`CANTIDAD_RECOMENDACIONES`). Es una selección acotada, no un listado navegable. Solo se recomienda para el usuario activo; otro `userId` da `PERMISOS`.
6. **La búsqueda no exige usuario activo**, porque no modifica información (igual que el caso de uso de #16).
7. **Clases comunes de controllers:** `ControllerExecutor`, `ControllerUser` y el cambio en `UiOperationExecutor` son idénticos a los de PR #63, para que la integración de ambos PRs no genere conflictos en esos archivos.
8. **Vista mínima, sin rediseño:** Explorar suma, en una segunda barra de filtros, modalidad, habilidad, precio mínimo y máximo, y reputación mínima; también el orden "Más recientes", "Página X de Y" y los botones Anterior/Siguiente. Explorar pide páginas de 3 publicaciones (una fila de tarjetas), así la paginación se ve con los datos de demo. Inicio no cambia.

## Errores

Todos se traducen con `UiOperationExecutor` a `PresentationException`:

| Situación | Código |
| --- | --- |
| Filtros nulos, orden desconocido, precio negativo o rango invertido, reputación fuera de [0, 5], página negativa, tamaño fuera de [1, 100], texto de más de 100 caracteres | `VALIDACION` |
| Recomendar sin usuario activo | `NO_AUTENTICADO` |
| Recomendar para un usuario distinto del activo | `PERMISOS` |

## Alternativas descartadas

- **Separar `recomendar` en un `RecomendacionController` propio**, como lista §9 de la consigna (que aclara que los nombres son orientativos): se descartó para no cambiar el contrato que usa la vista ni sumar conflictos con #63 y #64. Si el equipo lo decide, se separa sin tocar los casos de uso.
- **Paginar las recomendaciones:** son las mejores N publicaciones para el usuario. Una página 2 no tiene significado para la estrategia y la pantalla las muestra como selección.
- **Un modelo de página genérico (`Pagina<T>`):** hoy solo hay un listado paginado en la UI, y el contrato de presentación admite colecciones y records simples. Se agrega cuando otro listado lo necesite.

## Impacto en ramas abiertas

- **PR #70 (#16):** esta rama parte de ella. Cuando #70 se mergee con squash, hay que rebasar con `git rebase --onto origin/main feature/16-busqueda-filtrada`.
- **PR #63 (#28) y PR #64 (#31):**
  - `ControllerExecutor`, `ControllerUser` y `UiOperationExecutor` son idénticos, sin conflicto;
  - `UiControllers`, `WiredUiControllers`, `MockSkillSwapControllers` y `SkillSwapApplicationWiring` cambian en ambos lados y requieren resolver conflictos al integrar;
  - PR #63 usa `mapper.toPublicacion(view, autor, 0)`, que #16 reemplazó por `toPublicacion(view)`.

## Evidencia

| Prueba | Cubre |
| --- | --- |
| `BusquedaUiControllerTest` | Conversión de todos los filtros, orden y página; cada etiqueta de orden; página con autor y navegación; errores de filtros y paginación sin llamar al caso de uso; categorías; recomendaciones con cantidad fija, usuario activo, otro usuario y sin identidad. |
| `WiredUiControllersTest` | Integración: páginas con total y navegación, filtros nuevos, error de validación, recomendación para otro usuario. |
| `MockSkillSwapControllersTest` | El mock cumple el contrato paginado y los filtros nuevos. |
| `UiModelMapperTest` | "Más recientes" y orden desconocido. |
| `PresentationContractsTest` | `PaginaPublicaciones` no expone entidades. |
