# Trazabilidad para la defensa: patrones, GRASP y arquitectura

Matriz de seguimiento del issue [#35](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/issues/35). Revisión del 02/11/2026 sobre `feature/persistence-infrastructure`, partiendo de `d0f036a` (PR #61), que incluye `main` en `9752c91` y la decisión del PR #66. La evidencia de esta rama no implica que #61 esté integrado en `main`. Esta actualización es documental; no crea ni modifica UML.

Las secciones señaladas como **históricas** conservan el inventario de `main` en `36b07b0` (26/10/2026, PR #48–#52) para trazabilidad. Sus pendientes no describen el estado actual. El inventario de patrones y la actualización siguiente distinguen implementación disponible de validación pendiente.

## Actualización del PR #61

- Application ya dispone de servicios concretos y pruebas bajo `application/service`; la demo los conecta mediante `SkillSwapApplicationWiring` y repositorios en memoria.
- Infrastructure incorpora entidades, mappers, repositorios JPA, `JpaTransactionManager` y migraciones V1/V2. `JpaBookingClientMessageTest` verifica guardar, buscar y actualizar el mensaje y aplicar V2 a una tabla con filas previas en H2 modo MySQL.
- Adapter está implementado para identidad y notificaciones; Observer tiene un dispatcher, suscripciones y pruebas de varios observadores. La evidencia concreta se detalla abajo.
- La verificación posterior contra MySQL real/Flyway, rollback, carreras de reservas/pagos y paginación se registra en [Pruebas de MySQL](pruebas-mysql.md). La demo en memoria y H2 no sustituyen esa suite. La prueba no certifica carga de producción ni todos los intercalados posibles.
- Los controllers de #63/#64 y la integración/calificación de #65 están en ramas separadas; este corte no certifica su integración ni el recorrido visual completo. La cobertura Domain/Application todavía requiere medición.
- Verificación inicial del 02/11/2026: `mvn clean test` sobre `d0f036a`, 117 pruebas, 0 fallos, 0 errores y 0 omitidas. La revisión documental `e634a02` fue seguida por las pruebas reales de MySQL, sus correcciones de Infrastructure y V3; consultar el registro enlazado para la validación posterior.

## Criterio de auditoría

La consigna pide al menos cuatro patrones justificados y probados; los patrones sugeridos son opciones, no una lista que deba implementarse artificialmente. El inventario permite ver todos los patrones tratados en la cursada y diferencia su evidencia del trabajo pendiente. Los patrones verificados indican implementación y pruebas para el alcance indicado, no que la aplicación completa ya esté integrada.

## Requisitos funcionales, arquitectura y estado (histórico: 26/10)

| Área / issue | Evidencia integrada en `main` | Estado y próximo trabajo |
| --- | --- | --- |
| Perfiles, habilidades, intereses, disponibilidad y baja lógica (#7, #14) | `domain/model/Profile`, `AvailabilitySlot` y `DomainRulesTest` validan titularidad, perfil activo, intervalos y solapamientos. | **Dominio implementado; flujo pendiente.** Falta orquestación de aplicación y persistencia. |
| Publicaciones, borradores y transiciones (#8, #15, #36) | `Publication`, `PublicationBuilder`, `PublicationState`, `PublicationStates`; `PublicationBuilderTest` y `DomainRulesTest`. | **Reglas locales implementadas.** Falta el caso de uso concreto, almacenamiento y conexión con la UI. |
| Búsqueda, filtros, orden y paginación (#16) | `DiscoveryUseCases`, `SearchQuery` y `PageResult` definen el contrato y validan criterios/paginación. | **Contrato integrado; función pendiente.** Falta el servicio de aplicación y consulta paginada en persistencia. |
| Recomendaciones (#12, #17) | `RecommendationStrategy`, `SkillAffinityStrategy`, `RecentPopularityStrategy`; pruebas en `DomainRulesTest`. | **Strategy implementado y probado en dominio.** Falta su consumo desde un caso de uso integrado. |
| Contrataciones, pagos, cancelación y concurrencia (#18–#20, #24) | `Booking`, `BookingState`, `BookingStates`, `BookingRepository` y `TransactionManager` declaran reglas y contratos; las pruebas de dominio cubren transiciones. | **Dominio y puertos integrados.** Falta el servicio que coordine reservas, fondos y transacciones; no hay bloqueo ni garantía persistente ante concurrencia/doble pago. |
| SkillCoins y billetera (#10, #19) | `SkillCoins`, `Wallet`, `LedgerEntry` y pruebas de saldo/movimientos. | **Reglas locales implementadas.** Falta persistir y coordinar débito/crédito atómicamente. |
| Calificaciones y reputación (#11, #21) | `Rating` valida contratación completada y participación; `Profile` deriva reputación; `RatingRepository` define la consulta de unicidad. | **Dominio y puerto integrados.** Falta el caso de uso y una restricción persistente única por contratación/autor. |
| Notificaciones y eventos (#21, #25) | `NotificationUseCases`, `NotificationRepository`, `NotificationGateway`, `DomainEventPublisher` y modelos de mensaje. | **Contratos integrados.** No hay publicación de eventos, suscriptores, persistencia de bandeja ni adaptadores reales. El puerto por sí solo no constituye Observer o Adapter implementados. |
| Puertos y modelos de Application (#4, #14–#21) | Interfaces de entrada y salida, comandos, consultas y resultados bajo `application/`. | **Contratos integrados por PR #48.** Los servicios concretos y sus pruebas todavía están pendientes en los issues de casos de uso. |
| JPA, MySQL, migraciones y transacciones (#5, #22–#29) | Hibernate, MySQL y Flyway están declarados como dependencias previstas en `pom.xml`; `application/port/out` contiene contratos. | **Persistencia pendiente.** No hay entidades JPA, implementaciones concretas, migraciones ni pruebas de repositorio/concurrencia. |
| Presentation / Mock UI Kit (#6, #27–#32) | `SkillSwapView` consume `UiControllers`; `MockSkillSwapControllers` simula datos en memoria. README y guion de demo (#51) describen alcance y recorrido. | **Scaffold mock disponible.** No hay controllers reales conectados a Application; al reiniciar se restablece el estado de demo. |
| UML y decisiones de diseño (#6) | `docs/uml-dominio.puml`, `docs/uml-aplicacion.puml`, decisiones de State/Builder/Application y este inventario. | **Este cambio agrega** el proyecto editable `docs/modelo-skill-swap.mdj` y `output/pdf/informe-diseno-skillswap.pdf`; revisar ambos junto con el código. |
| Calidad y pruebas | Pruebas unitarias de dominio, Builder, promociones y mock UI. | **Parcial.** Faltan pruebas de casos de uso con puertos simulados, persistencia, integración y un reporte de cobertura reproducible. |

## Inventario de patrones

Las filas que dicen «en `main`» conservan evidencia del corte histórico; Adapter, Observer, Simple Factory y Factory Method se actualizan al PR #61. La ausencia histórica de servicios o adapters debe leerse junto con la actualización del PR #61, no como una ausencia en esta rama.

| Patrón | Aplicación evaluada | Evidencia / conclusión |
| --- | --- | --- |
| Strategy | Variantes de recomendación y cálculo de reintegro. | **Verificado en `main`:** `RecommendationStrategy` y `RefundPolicy`, con dos variantes cada uno y pruebas de dominio. No hay todavía caso de uso que seleccione/consuma las estrategias. |
| State | Ciclo de vida de publicaciones y contrataciones. | **Verificado en `main`:** contratos e implementaciones en `domain/state`; tests de transiciones y documentación de #50. |
| Builder | Crear publicaciones con datos obligatorios y opcionales. | **Verificado en `main`:** `PublicationBuilder`, `PublicationBuilderTest` y decisión #36; PR #49 está integrado. |
| Decorator | Componer promociones sobre un precio base. | **Verificado en `main`:** `PriceQuote`, `PriceDecorator`, `FixedAmountPromotion`, `PercentagePromotion` y `PromotionDecoratorTest` (#46). |
| Singleton | Reusar estados inmutables sin estado por contratación/publicación. | **Uso acotado en `main`:** los enums de `BookingStates` y `PublicationStates` tienen una instancia por estado. La decisión #50 justifica no crear un Singleton mutable ni un Singleton de aplicación. |
| Adapter | Adaptar una biblioteca/proveedor externo a un puerto interno. | **Implementado en PR #61 (#25), con proveedores mock:** `IdentityProviderAdapter` traduce `MockIdentityClient.getAuthenticatedIdentity().subject()` a `CurrentUserProvider.currentUserId()`; `EmailNotificationAdapter` y `PushNotificationAdapter` convierten `NotificationMessage` a las llamadas de sus clientes mock. `InAppNotificationAdapter` guarda en el repositorio. Evidencia: `IdentityProviderAdapterTest`, `EmailNotificationAdapterTest`, `PushNotificationAdapterTest` y `NotificationFactoryTest`, bajo `src/test/java/ar/edu/uade/skillswap/infrastructure/`. Los puertos por sí solos no constituyen Adapter; aquí sí hay implementaciones y traducción. No se afirma integración con proveedores reales. |
| Observer | Avisar a varios interesados a partir de un evento de dominio. | **Implementado en PR #61:** `InMemoryDomainEventPublisher.subscribe/publish` registra y notifica observadores; `SkillSwapApplicationWiring.subscribeNotifications()` suscribe `NotificationEventObserver.onEvent` a eventos de contratación y `RatingNotificationEventPublisher.publish` a `rating.submitted`. `NotificationEventObserverTest` prueba notificación al otro participante, entrega a un segundo observador y continuidad ante el fallo de uno; `RatingNotificationEventPublisherTest` prueba preferencias y canales. Es síncrono y en memoria: no garantiza entrega durable ni atomicidad con JPA. |
| Simple Factory | Seleccionar una implementación de notificación según canal/preferencias. | **Implementado en PR #61 (#25):** `NotificationFactory.create(channel)` selecciona Email, Push o InApp; `NotificationFactoryTest` cubre cada variante. Decisión detallada en `docs/decision-factory-method-vs-simple-factory.md`. |
| Factory Method | Permitir que una subclase creadora determine el producto concreto. | **Evaluado y descartado (#41):** no hay variación real por subtipos de creadores; no se duplica la Simple Factory. Justificación en `docs/decision-factory-method-vs-simple-factory.md`. |
| Abstract Factory | Crear familias relacionadas de objetos de una misma variante de proveedor. | **Evaluación pendiente:** el alcance actual no muestra una familia concreta que la necesite. Documentar el descarte si no surge un proveedor con productos relacionados. |
| Composite | Tratar categorías o habilidades compuestas y hojas de forma uniforme. | **Pendiente (#37):** no existe jerarquía recursiva en el modelo actual. |
| Facade | Simplificar una consulta pública que coordina varias fuentes. | **Pendiente (#40):** no hay servicio de consulta integrado. No agregar una fachada sin el flujo que deba simplificar. |
| MVC | Separar vista, controller y modelo/casos de uso. | **Parcial:** el Mock UI Kit tiene vista, contrato de controllers y mock; faltan controllers reales y la integración con Application. |
| Repository / DAO | Aislar el acceso a almacenamiento detrás de contratos. | **Contratos integrados:** interfaces de repositorio en Application. Las implementaciones concretas tipo JPA/DAO y su prueba en base real siguen pendientes. DAO se registra como forma de acceso a datos, no como patrón GoF. |
| CQRS | Separar consultas de comandos. | **Parcial/decisión pendiente (#42):** hay interfaces por área y métodos de lectura/escritura, pero no una separación formal de modelos o persistencias. No se necesita una segunda base para aplicar la idea. |

**Conteo de la consigna:** Strategy, State, Builder y Decorator son cuatro patrones GoF distintos con implementación y pruebas en `main`. Singleton se documenta como técnica localizada de los estados inmutables, no como patrón agregado para cumplir un conteo.

## GRASP y SOLID (histórico: 26/10)

| Principio | Evidencia | Estado |
| --- | --- | --- |
| Information Expert | `Profile`, `Publication`, `AvailabilitySlot`, `Booking`, `Wallet`, `SkillCoins` y `Rating` conocen y protegen sus propias reglas. | Implementado para invariantes locales; evitar duplicarlas en servicios. |
| Creator | Métodos de creación como `Profile.register`, `Publication.draft`, `Booking.request`, `Wallet.open` y el Builder de `Publication`. | Aplicado en dominio. |
| Controller | `UiControllers` recibe eventos del Mock UI Kit; `MockSkillSwapControllers` atiende la demo. | Parcial; controller productivo pendiente. |
| Polymorphism | State, Strategy y `PriceQuote` delegan variaciones tras interfaces. | Verificado con pruebas de dominio. |
| Low Coupling / Protected Variations | UI depende de contrato; Application define puertos; Domain no importa JPA/UI/servicios externos. | Parcial hasta completar el wiring y adaptadores de Infrastructure. |
| High Cohesion | Agregados protegen invariantes; policies/strategies varían comportamiento; modelos de Application transportan datos. | Aplicado en componentes existentes; conservar al crear servicios/implementaciones. |
| Pure Fabrication / Indirection | Servicios, mappers, transacciones y adaptadores pertenecen a Application/Infrastructure, no a entidades. | Contratos de Application presentes; implementaciones pendientes. |
| SRP | Cada agregado protege sus reglas; puertos separan áreas de casos de uso y dependencias externas. | Implementado a nivel de modelo/contratos. |
| OCP / LSP | Nuevas políticas, recomendaciones y promociones se agregan mediante contratos; variantes State respetan el protocolo. | Strategy, State y Decorator verificados por pruebas. |
| ISP | Interfaces de entrada y salida agrupadas por capacidades: perfil, publicación, booking, wallet, rating y notificaciones. | Contratos integrados. |
| DIP | Application declara puertos; adaptadores concretos deberían depender desde afuera de esos puertos. | Parcial: falta Infrastructure concreta. |

## Arquitectura y límites de las capas (histórico: 26/10)

- **Presentation:** `ui/SkillSwapView`, `controllers/UiControllers` y la implementación mock. La vista no debe recibir entidades del dominio; los controllers reales convierten entrada/salida y delegan a los puertos de Application.
- **Application:** los contratos de entrada y salida y modelos de frontera ya están en `application/`. Los services futuros implementan casos de uso y coordinan identidad, repositorios, políticas, transacciones y publicación de eventos. No deben duplicar invariantes de entidades.
- **Domain:** `domain/model`, `domain/state`, `domain/policy` y `domain/pricing`. Usa Java estándar y no conoce Presentation, JPA, infraestructura ni proveedores.
- **Infrastructure:** aún no hay implementación. Aquí corresponden mapeos/persistencia JPA, repositorios concretos, migraciones, transacciones y adaptadores de correo/identidad/eventos.

## Próximos pasos registrados en el corte histórico (26/10)

- [ ] Implementar services por área de Application y probarlos con repositorios/puertos simulados.
- [ ] Crear persistencia MySQL/JPA y migraciones Flyway; probar constraints, consultas y transacciones.
- [ ] Integrar controllers reales y el recorrido principal publicar → solicitar → aceptar → pagar → completar → calificar.
- [ ] Resolver carreras de reservas, pagos duplicados y unicidad de calificaciones desde el caso de uso y la base.
- [ ] Decidir si hay proveedores reales que justifiquen Adapter/Observer/fábricas; mantener documentados los descartes.
- [ ] Medir la cobertura sugerida de Domain y Application con un reporte reproducible.
- [ ] Alinear el informe y los diagramas con los cambios que entren en próximos PRs.

## Pull requests integrados en el corte

- [PR #48 - puertos y contratos de Application](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/48).
- [PR #49 - Builder de publicaciones](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/49).
- [PR #50 - evidencia State y decisión Singleton](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/50).
- [PR #51 - README y guion de demo](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/51).
- [PR #52 - trazabilidad y actualización de auditoría](https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03/pull/52).
