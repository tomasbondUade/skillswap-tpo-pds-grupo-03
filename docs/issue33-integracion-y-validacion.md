# Issue #33 — integración y entrega (sin UML)

## Objetivo
Componer los controllers, los casos de uso y los adapters; completar el recorrido de contratación en Swing y documentar las decisiones. El modelo StarUML y su exportación PDF quedan expresamente fuera de este trabajo, por solicitud de la responsable.

## Cambios realizados en la rama
- CalificacionUiController: DTO simple, identidad activa, validación y delegación a RatingUseCases.
- Sesión demo: DemoCurrentUserProvider permite alternar entre user-mateo, mentor-ana y mentor-lu para representar comprador/proveedor sin simular autenticación real.
- WiredUiControllers incorpora calificaciones y selección de identidad; MockSkillSwapControllers mantiene el contrato.
- Swing agrega selección de usuario demo y un diálogo para calificar contrataciones completadas.

## Validación pendiente
La rama depende de #64, #63 y #62. El PR #61, que incorpora Adapter, Simple Factory y JPA, todavía no está fusionado; hay que integrar y resolver incompatibilidades antes de certificar la aceptación. No se ha ejecutado `mvn clean test` ni se ha medido cobertura en esta rama. No afirmar que MySQL, concurrencia o el recorrido visual estén verificados.

## Matriz de aceptación
| Criterio | Estado |
| --- | --- |
| Composición y proveedores mock | Parcial: composición en memoria y demo; integración con PR #61 pendiente |
| Publicar → solicitar → aceptar → pagar → completar → calificar en Swing | Código de calificación/roles agregado; ejecución end-to-end pendiente |
| Pruebas de integración y persistencia | Pendiente |
| README reproducible | Pendiente de validar instalación y comandos |
| Informe de decisiones PDF | Archivo generado fuera del repositorio para revisión |
| Modelo StarUML .mdj y PDF | Excluido por solicitud; pendiente de entrega por separado |
| Cobertura 80% Domain/Application | Sin medición; pendiente |

## Comandos sugeridos para validación
```bash
mvn clean test
mvn exec:java
```
En el perfil de persistencia, configurar MySQL y Flyway antes de iniciar, comprobar migraciones y Hibernate validate; no asumir que la demo en memoria prueba persistencia.

## Notas para defensa
Los controllers sólo adaptan entrada/salida. Application coordina transacciones y puertos. Domain protege reglas de participantes, estados, cupos y calificaciones. Infrastructure encapsula persistencia, identidad y notificaciones.
