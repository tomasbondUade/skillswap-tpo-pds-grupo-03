# Issue #33 — integración y entrega (sin UML)

## Objetivo y base

Conectar controllers, casos de uso y adapters y completar contratación/calificación. La revisión del 05/11/2026 actualiza la base de #63 con main en 2d6e9fd, persistencia #61 y búsqueda paginada #71. #62 ya está integrado. Este PR conserva su base en #63 y permanece en Draft.

StarUML y su exportación quedan fuera de este trabajo por solicitud de la responsable. No se generan ni editan diagramas en esta revisión.

## Integración corregida

- Se conservan PaginaPublicaciones, filtros, paginación y los datos reales del autor de Application.
- Perfil, publicaciones, búsqueda, contrataciones y calificaciones delegan en sus controllers dedicados; la fachada no duplica reglas ni transiciones.
- DemoCurrentUserProvider conserva la identidad inyectada hasta que se elige otro usuario de demo. Envuelve el adapter existente tanto en memoria como en JPA, evitando sustituirlo por una identidad fija o hacer un cast incompatible con JPA.
- El selector admite Mateo, Ana y Lucía. Cambiar identidad refresca saludo, pie lateral, saldo y pantalla; los permisos continúan en Application/Domain.
- Se conserva el modo --persistence=jpa y el cierre de la fábrica JPA.
- Los fixtures usan PublicationView.author y ProfileRepository del contrato actual, con el perfil requerido y sin reputaciones inventadas.

## Validación de la revisión

La prueba específica de sesión pasó con tres ejecuciones: conserva la identidad inyectada, rechaza identidades inválidas sin cambiar la sesión y completa solicitar → aceptar → pagar → iniciar → completar → calificar desde WiredUiControllers sobre MySQL/JPA. También comprueba permisos de publicaciones, saldos de ambos participantes y reputación persistida.

Suite completa del 05/11/2026: `mvn -Pmysql-it test`, **237 pruebas, 0 fallos, 0 errores y 0 omitidas**, con MySQL 8.4.13. La rama incorpora la corrección `e49094b` de #63 y está libre de conflictos con esa base. La validación visual completa y la cobertura permanecen pendientes.

Las pruebas MySQL usan bases aleatorias aisladas y Flyway V1–V4. Comandos: mvn test y mvn -Pmysql-it test, con las variables documentadas en docs/pruebas-mysql.md. Los resultados históricos de 124 pruebas corresponden al corte anterior del 02/11 y no certifican esta integración.

## Pendientes de aceptación

| Criterio | Estado |
| --- | --- |
| Composición memoria/JPA, mocks y selector | Integrados; prueba específica verificada |
| Recorrido mediante controllers/casos de uso sobre JPA | Verificado automáticamente; no equivale a interacción visual |
| Recorrido visual completo en Swing | Pendiente de ejecución y registro manual |
| Migraciones, rollback, concurrencia y consultas | Cubiertos por la suite de integración; no certifican carga de producción |
| Informe de decisiones final | Pendiente de completar y entregar |
| Cobertura 80% Domain/Application | Pendiente de medición |
| StarUML y PDF | Excluidos de este trabajo; entrega por separado |

Los checks remotos contra main y la aprobación se requieren al actualizar la base después de integrar #63. La prueba local no reemplaza esos controles. Este PR no se presenta como aceptación completa ni se fusiona en esta revisión.
