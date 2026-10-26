# Control de patrones, GRASP y arquitectura

Este documento es la matriz de seguimiento del issue [#35](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/issues/35). Se actualiza a medida que los cambios de cada integrante se integran en `main`.

## Criterio de auditoría

La consigna pide aplicar **al menos cuatro patrones**, justificados por variaciones reales y respaldados por pruebas; no exige implementar todos los patrones sugeridos. El equipo mantiene el inventario completo para controlar lo visto en la cursada, pero una decisión de no aplicar un patrón debe quedar analizada y justificada. En particular, Singleton solo se incorpora si existe una necesidad real y se explican sus consecuencias.

Estados usados en esta matriz:

- **Pendiente:** todavía no hay evidencia revisada en `main`.
- **Parcial:** existe una base o una parte del flujo, pero falta completar o verificar el criterio.
- **Verificado:** se revisaron implementación, pruebas y documentación/UML.
- **Descartado con justificación:** se evaluó y se documentó por qué no aporta al diseño.

Tener un issue asignado o una clase con el nombre del patrón no alcanza para marcarlo como verificado.

## Patrones y conceptos arquitectónicos

| Patrón o concepto | Caso e issue | Evidencia que se debe revisar | Estado en la base del repositorio |
| --- | --- | --- | --- |
| Strategy | Recomendación (#12) y reintegros (#13) | Interfaz común, algoritmos intercambiables, selección sin condicionales crecientes y pruebas por estrategia. | Pendiente |
| State | Ciclo de contratación (#9) | Estados con transiciones válidas, acciones permitidas y rechazadas, historial y pruebas. | Pendiente |
| Observer | Eventos de negocio y notificaciones (#21, coordinación con #25) | Emisión de eventos desacoplada, más de un suscriptor/canal y pruebas de publicación/reacción. | Pendiente |
| Adapter | Identidad y proveedores de notificación (#25) | Puerto que necesita la aplicación, interfaz real/incompatible del proveedor y traducción aislada en Infrastructure. | Pendiente |
| Simple Factory | Selección de canal de notificación (#25) | Creación centralizada de implementaciones concretas; no debe confundirse con Factory Method. | Pendiente |
| Abstract Factory | Evaluación de familias de productos relacionadas | Las notas mencionan el patrón como ejemplo de posible sobreingeniería; revisar si la consigna ofrece una familia real que deba crearse en conjunto y, si no, justificar el descarte. | Pendiente de evaluación |
| Factory Method | Variación del proceso de creación (#41) | Una operación de creación extensible mediante implementaciones; demostrar una variación real o dejar justificado por qué no se usa. | Pendiente |
| Builder | Borradores de publicaciones (#36) | Construcción paso a paso; no permite omitir datos obligatorios y delega invariantes a la entidad de dominio. | Pendiente |
| Decorator | Promociones acumulables (#34) | Composición de promociones sin mutar el precio base y con total nunca negativo; pruebas de uno, varios y límite cero. | Pendiente |
| Composite | Jerarquía de categorías/habilidades (#37) | Elementos simples y compuestos tratados uniformemente; pruebas de composición y recorrido. | Pendiente |
| Facade | Consulta del perfil público (#40) | Fachada de aplicación que coordina las fuentes necesarias sin mover reglas de negocio fuera del dominio. | Pendiente |
| Singleton | Evaluación de instancia única (#9) | Solo verificar si hay necesidad de una instancia única; si no la hay, registrar el descarte justificado y evitar estado mutable global. | Pendiente de decisión |
| MVC | Presentación e integración (#6, #27–#32) | La vista depende de interfaces de controller; los controllers delegan y no contienen reglas ni persistencia. El Mock UI Kit es solo una base. | Parcial: scaffold provisto; integración real pendiente |
| DAO / Repositories | Persistencia (#29) | Contratos de repositorio separados de implementaciones JPA; consultas y persistencia aisladas en Infrastructure. DAO se registra como concepto de persistencia, no como patrón GoF. | Pendiente |
| CQRS | Evaluación de consultas y comandos (#42) | Decisión explícita sobre separación de lecturas y escrituras; aplicar una variante liviana solo si aporta, o justificar su descarte. | Pendiente |

> En el scaffold público actual, `SkillSwapView` consume interfaces de `UiControllers` y `MockSkillSwapControllers` mantiene datos en memoria. Esto acredita una base de integración de la UI, no la implementación final de los casos de uso, las reglas del dominio o la persistencia.

## GRASP

La evidencia final debe apuntar a clases/métodos concretos del código y reflejarse en el UML y el informe. Los issues indicados son referencias de trabajo, no evidencia por sí solos.

| Principio | Pregunta de auditoría | Evidencia esperada | Referencias de trabajo |
| --- | --- | --- | --- |
| Information Expert | ¿El objeto con la información necesaria realiza la regla? | Entidades/value objects del dominio validan sus invariantes; los controllers no calculan reglas. | #3, #7–#11 |
| Creator | ¿La creación se asignó a quien tiene los datos o la relación adecuada? | Creación en entidad/fábrica/caso de uso según responsabilidad, sin constructor central omnisciente. | #3, #4, #8–#10, #36 |
| Controller | ¿Quién recibe el evento del sistema y coordina el caso de uso? | Controller de presentación delega al caso de uso; no concentra negocio ni acceso a datos. | #6, #27–#32 |
| Low Coupling | ¿Se evitaron dependencias concretas innecesarias? | Application depende de puertos; Infrastructure implementa repositorios/adaptadores; Domain no conoce frameworks. | #3–#6, #22–#29 |
| High Cohesion | ¿Cada componente tiene responsabilidades relacionadas? | Casos de uso orquestan; entidades mantienen reglas; mappers y persistencia quedan en sus capas. | #3–#6, #27–#33 |
| Polymorphism | ¿Una variación de comportamiento se resuelve por tipos intercambiables? | Strategy, State u otra abstracción necesaria reemplaza condicionales por tipo y tiene pruebas. | #9, #12, #13, #21, #25 |
| Pure Fabrication | ¿Se asignó una responsabilidad técnica a un objeto no propio del dominio? | Servicios de aplicación, mappers y persistencia mantienen cohesión sin contaminar entidades. | #4, #5, #23, #26, #29 |
| Protected Variations | ¿Los puntos de cambio están protegidos detrás de una abstracción estable? | Puertos para proveedores externos y políticas intercambiables con implementaciones reemplazables. | #5, #12, #13, #25, #29 |
| Indirection | ¿Un intermediario reduce el acoplamiento entre dos componentes? | Puertos/adaptadores median entre casos de uso, persistencia y proveedores externos. | #4, #5, #25, #29 |

## Arquitectura y cierre

- [ ] El dominio contiene entidades, value objects y reglas, sin dependencias de UI, JPA/Hibernate, base de datos o proveedores externos.
- [ ] Application coordina casos de uso y depende de abstracciones.
- [ ] Los contratos de repositorio están separados de sus implementaciones de Infrastructure.
- [ ] Presentation convierte entradas/salidas; no expone entidades de dominio como DTOs.
- [ ] Cada patrón que se marque como verificado tiene evidencia de código, pruebas y UML/informe.
- [ ] Los diagramas muestran clases/interfaces relevantes, responsabilidades, relaciones y multiplicidades.
- [ ] La decisión sobre cada patrón sugerido queda registrada: aplicación justificada o descarte justificado.
- [ ] Se puede recorrer un caso de uso completo y explicar las colaboraciones en la defensa.

## Línea de base

Esta matriz describe el estado inicial de la rama del repositorio usada para este control. Debe actualizarse cuando se integren los PRs de implementación. No se cierra el issue #35 por la creación de esta matriz: el cierre requiere revisar la evidencia final del equipo.
