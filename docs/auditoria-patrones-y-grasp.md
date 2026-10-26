# Trazabilidad para la defensa: patrones, GRASP y arquitectura

Matriz de seguimiento del issue [#35](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/issues/35). Relaciona los requisitos de la consigna con la evidencia que existe en `main` y con el trabajo que todavía está pendiente.

## Alcance y criterio

Este estado corresponde a `main` en el commit `379a654` y a los PRs abiertos #48–#51 consultados el 26/10/2026. Un PR abierto se registra como trabajo en revisión, no como funcionalidad ya integrada.

La consigna exige **al menos cuatro patrones** justificados y probados; aclara que no es obligatorio implementar todos los patrones sugeridos. El inventario de abajo permite revisar lo visto en clase, pero cada patrón debe implementarse solo cuando resuelva una necesidad concreta. Los patrones que no se apliquen requieren una justificación. Singleton tiene además una condición explícita: solo se usa si hace falta una única instancia.

Estados de seguimiento:

- **Verificado en `main`:** hay implementación y evidencia de pruebas/documentación apropiada para el alcance indicado.
- **Parcial en `main`:** existe una parte, pero falta completar su flujo o evidencia.
- **En revisión:** la evidencia está en un PR abierto y todavía no forma parte de `main`.
- **Pendiente:** no se encontró evidencia suficiente en `main` ni en los PRs consultados.
- **Evaluación pendiente:** el equipo debe decidir si existe un caso real o justificar que no se aplica.

## Trazabilidad de requisitos funcionales y capas

| Requisito de la consigna | Evidencia actual | Estado y próximo trabajo |
| --- | --- | --- |
| Perfil, habilidades, intereses, disponibilidad y baja lógica (#7, #14) | `Profile` y `AvailabilitySlot` concentran validaciones, disponibilidad y baja lógica. `DomainRulesTest` cubre propiedad, solapamientos y perfil dado de baja. | **Parcial en `main`:** el modelo de dominio está; falta el caso de uso y su persistencia. |
| Publicaciones y transiciones (#8, #15) | `Publication` protege borradores, disponibilidad, permisos, solicitudes y transiciones de estado; el State está en `domain/state`. | **Parcial en `main`:** el dominio está; falta completar casos de uso, almacenamiento e integración con la UI. El Builder está **en revisión** en [PR #49](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/49). |
| Búsqueda, filtros, orden y paginación (#16) | Hay modelos de publicación y estrategias de recomendación; no hay búsqueda paginada conectada en `main`. | **Pendiente:** falta el caso de uso y una consulta eficiente en persistencia, con paginación en base de datos. |
| Recomendaciones intercambiables (#12, #17) | `RecommendationStrategy`, `SkillAffinityStrategy` y `RecentPopularityStrategy`; `DomainRulesTest` compara ambas reglas. | **Verificado en `main`** para las estrategias de dominio; falta el caso de uso consumidor y la integración. |
| Solicitud, reserva, contratación y concurrencia (#18–#20, #24) | `Booking`, `Publication` y los estados cubren invariantes locales, participantes e historial; las pruebas unitarias validan transiciones y disponibilidad. | **Parcial en `main`:** no están integradas las transacciones de aceptación/pago/cancelación ni una protección persistente frente a concurrencia y doble cobro. |
| SkillCoins y wallet (#10, #19) | `SkillCoins` y `Wallet` validan montos/saldo y registran movimientos inmutables; hay pruebas de saldo insuficiente y consistencia del ledger. | **Parcial en `main`:** falta coordinar débito y crédito dentro de una transacción de aplicación/infraestructura. |
| Calificación y reputación (#11, #21) | `Rating` valida contratación completada y participantes; `Profile` deriva su reputación desde calificaciones. | **Parcial en `main`:** falta el caso de uso y garantizar una única calificación por persona y contratación mediante la coordinación con persistencia. |
| Eventos y notificaciones (#21, #25) | Hay preferencias/tipos de canal en el modelo. | **Pendiente:** faltan publicación de eventos, suscriptores y adaptadores mock de canales externos. |
| Puertos y casos de uso de Application (#4, #14–#21) | `main` todavía no contiene los contratos de Application. | **En revisión:** [PR #48](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/48) agrega puertos, comandos/modelos y documentación; los servicios y sus pruebas siguen asignados a issues específicos. |
| JPA, MySQL, migraciones, repositorios y transacciones (#5, #22–#29) | No se encontraron implementaciones concretas de persistencia ni migraciones en `main`. | **Pendiente:** implementar en Infrastructure, incluidas restricciones, consultas, datos demo, límites transaccionales y concurrencia. |
| Controllers delgados e integración del Mock UI Kit (#6, #27–#32) | `SkillSwapView` consume `UiControllers`; `MockSkillSwapControllers` es el adaptador de demostración en memoria. | **Parcial en `main`:** está el scaffold mock; conectar controllers reales, casos de uso, conversión de DTOs y configuración. [PR #51](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/51) agrega README y guion de demo, no la integración completa. |
| Pruebas y calidad | Hay pruebas unitarias de reglas del dominio, estrategias y promociones. | **Parcial:** faltan pruebas de Application con puertos simulados, persistencia/integración, casos de aceptación y medición de cobertura sugerida. |
| UML, informe y entrega | El repositorio contiene `docs/uml-dominio.puml` y documentación Markdown. El diagrama de Application se propone en PR #48. | **Pendiente de cierre:** la consigna solicita el modelo en StarUML y un informe PDF; verificar/agregar ambos formatos como artefactos de entrega. |

## Patrones de diseño

| Patrón | Variación o caso | Evidencia / issues | Estado en `main` y siguiente paso |
| --- | --- | --- | --- |
| Strategy | Algoritmos de recomendación y políticas de reintegro. | `RecommendationStrategy`, dos implementaciones; `RefundPolicy`, dos políticas; `DomainRulesTest`; #12–#13. | **Verificado en `main`** a nivel de dominio y pruebas unitarias. El caso de uso que selecciona/consume cada estrategia sigue pendiente. |
| State | Transiciones de publicación y contratación. | `PublicationState`, `BookingState`, implementaciones y pruebas de transiciones; #8–#9. | **Verificado en `main`** para comportamiento y reglas. [PR #50](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/50) agrega evidencia de aislamiento y documentación específica sobre las instancias compartidas. |
| Observer | Un evento de negocio notifica a más de un interesado/canal. | #21 y coordinación con #25. | **Pendiente:** demostrar publicación desacoplada, múltiples suscriptores y pruebas. No confundir el puerto de envío con el patrón completo. |
| Adapter | Traducir interfaces de identidad/notificaciones externas a puertos internos. | #25 y #5. | **Pendiente:** hacen falta un puerto requerido por Application y un proveedor externo/mock incompatible cuya traducción quede en Infrastructure. |
| Simple Factory | Elegir/crear un canal de notificación según preferencias. | #25. | **Pendiente:** implementar si hay más de una implementación concreta que seleccionar; documentar su diferencia con Factory Method. |
| Factory Method | Variación real del proceso de creación. | #41. | **Evaluación pendiente:** aplicarlo solo si una subclase/implementación debe decidir qué producto crear; no duplicar la Simple Factory para contar patrones. |
| Abstract Factory | Creación coordinada de familias de objetos relacionados. | Control de patrones #35; revisar el material de la cursada si se exige auditarlo. | **Evaluación pendiente:** la consigna del TP no muestra por sí sola una familia concreta que lo requiera; justificar descarte si no aparece tal necesidad. |
| Builder | Construir publicaciones con datos obligatorios y opcionales. | #36. | **En revisión:** la implementación y pruebas están en [PR #49](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/49), todavía no en `main`. |
| Decorator | Componer promociones sobre un precio sin modificar el precio base. | `PriceQuote`, `PriceDecorator`, promociones y `PromotionDecoratorTest`; #34. | **Verificado en `main`** para composición, acumulación y total no negativo. |
| Composite | Tratar categorías/ habilidades hoja y compuestas uniformemente. | #37. | **Pendiente:** falta la jerarquía recursiva, recorrido, restricciones y pruebas. |
| Facade | Simplificar la consulta de perfil público que reúne varias fuentes. | #40. | **Pendiente:** falta la consulta coordinadora con proyección de aplicación, privacidad y pruebas. |
| Singleton | Una instancia única para estados inmutables, solo si aporta valor. | Estados enum en `BookingStates`; #9. | **Parcial:** `Booking` no es Singleton ni comparte datos; los estados son instancias únicas inmutables. [PR #50](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/50) documenta y prueba esa decisión. No agregar un Singleton mutable. |
| MVC | Separar vista, controller y modelo/casos de uso. | `SkillSwapView`, `UiControllers`, mock; #6 y #27–#32. | **Parcial:** la UI consume interfaces y el mock permite demo; falta conectar controllers reales y probar el recorrido. |
| Repository / DAO | Aislar persistencia detrás de contratos. | Puertos de repositorio propuestos en PR #48; #4–#5 y #22–#29. | **Parcial/en revisión:** los contratos están en PR #48; las implementaciones JPA y pruebas de persistencia están pendientes. DAO se registra como concepto de acceso a datos, no como patrón GoF. |
| CQRS | Separación de consultas y comandos. | #42. | **Evaluación pendiente:** decidir y documentar si la separación aporta valor; no requiere una segunda base de datos. La paginación de consultas debe resolverse en persistencia. |

**Conteo para la consigna:** en `main` se reconocen al menos tres patrones GoF con implementación y pruebas: Strategy, State y Decorator. Si se aprueba e integra el Builder del PR #49, queda evidencia de cuatro patrones distintos. La decisión final debe considerar la aprobación/revisión y mantener UML e informe alineados.

## GRASP y responsabilidades

| Principio | Evidencia actual | Estado / trabajo restante |
| --- | --- | --- |
| Information Expert | `Profile`, `Publication`, `Booking`, `Wallet` y `SkillCoins` protegen reglas sobre sus propios datos. | **Parcial:** las invariantes locales están en el dominio; validar en la integración que Application no las replique. |
| Creator | Fábricas de dominio como `Profile.register`, `Publication.draft`, `Booking.request` y `Wallet.open` construyen objetos que protegen sus invariantes. | **Presente en dominio:** documentar en UML las relaciones y responsabilidades. |
| Controller | `UiControllers` define el contrato consumido por la vista; el mock recibe las acciones de demostración. | **Parcial:** implementar controllers reales que deleguen a Application. |
| Low Coupling | La UI usa una interfaz; el dominio usa Java estándar; los puertos de Application aparecen en PR #48. | **Parcial:** aprobar/integrar los puertos y completar implementaciones en Infrastructure. |
| High Cohesion | Entidades, políticas, estrategias y cotizaciones tienen responsabilidades delimitadas en Domain. | **Parcial:** conservar esa división al agregar servicios, mappers, repositorios y controllers. |
| Polymorphism | Strategy y State delegan comportamiento variable a contratos/estados concretos; promociones comparten `PriceQuote`. | **Verificado en dominio**; mantener pruebas al integrar los casos de uso. |
| Pure Fabrication | Se prevén servicios de aplicación, mappers y adaptadores para responsabilidades de coordinación/técnicas. | **Pendiente de implementación:** no ubicar persistencia ni conversión de UI en las entidades. |
| Protected Variations e Indirection | Interfaces de controller en el scaffold; puertos de Application en PR #48; Strategy protege cambios de algoritmo. | **Parcial:** falta que persistencia y proveedores implementen puertos y que la configuración ensamble las dependencias. |

## Pendientes concretos para cerrar la defensa

- [ ] Revisar/aprobar e integrar PRs #48–#51; no presentar su contenido como ya integrado antes del merge.
- [ ] Actualizar esta matriz cuando se integren PRs o aparezcan nuevas pruebas; los patrones verificados deben apuntar a clases, pruebas y diagramas.
- [ ] Completar Application e Infrastructure: casos de uso, JPA/MySQL, migraciones, transacciones y concurrencia.
- [ ] Conectar controllers reales a la UI y cubrir el recorrido principal: publicar → solicitar → aceptar → pagar → completar → calificar.
- [ ] Incorporar el modelo StarUML y el informe PDF de decisiones que pide la consigna; incluir consecuencias y evidencia de código/pruebas.
- [ ] Confirmar el requisito de cobertura sugerida del 80 % en Domain y Application con un reporte reproducible.
- [ ] Preparar para cada integrante una explicación de una regla de dominio, una colaboración entre capas y un patrón asignado.

## PRs abiertos incluidos en este corte

- [PR #48 — puertos y contratos de Application](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/48).
- [PR #49 — Builder de publicaciones](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/49).
- [PR #50 — evidencia State y decisión Singleton](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/50).
- [PR #51 — README y guion de demo](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/51).
