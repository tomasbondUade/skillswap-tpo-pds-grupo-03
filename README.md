# SkillSwap

SkillSwap es un marketplace académico para ofrecer y contratar mentorías. Este repositorio corresponde al Trabajo Práctico de Proceso de Desarrollo de Software.

## Estado actual

La aplicación ejecutable es una demo de escritorio en Java Swing. La vista usa controllers que conectan perfiles, publicaciones, búsqueda, contrataciones, billetera y notificaciones con servicios de Application y repositorios en memoria. `MockSkillSwapControllers` permanece disponible para pruebas y recorridos aislados.

La demo no persiste datos y no requiere una base MySQL configurada. Al cerrar y volver a abrir la aplicación, los datos vuelven a su estado inicial. Los cambios de saldo y las transiciones solo representan SkillCoins ficticios; no hay pasarela de pago ni garantías de transacción/concurrencia de producción.

## Requisitos

- JDK 17 o superior.
- Apache Maven 3.9 o superior.
- Git.

No se necesitan variables de entorno ni credenciales para ejecutar la demo.

## Obtener el proyecto

```bash
git clone https://github.com/tomasbondUade/skillswap-tpo-pds-grupo-03.git
cd skillswap-tpo-pds-grupo-03
```

## Compilar y probar

```bash
mvn clean test
```

Maven compila con Java 17 y ejecuta las pruebas automatizadas con JUnit.

## Ejecutar la aplicación

```bash
mvn exec:java
```

Se abre la ventana **SkillSwap · Mock UI Kit**. Para cerrarla, cerrá la ventana normalmente.

## Recorrido de demostración

El guion detallado está en [`docs/guion-demo.md`](docs/guion-demo.md). Describe los flujos conectados y las limitaciones que siguen pendientes, incluida la identidad fija de demo y la ausencia de persistencia real.

## Diseño y defensa

- [Modelo editable de clases en StarUML](docs/modelo-skill-swap.mdj), con diagramas de Domain y Application.
- [Informe de decisiones de diseño](docs/informe-diseno.md) y [versión PDF](output/pdf/informe-diseno-skillswap.pdf).
- [Auditoría de patrones, GRASP y SOLID](docs/auditoria-patrones-y-grasp.md), con estado verificado en `main` y pendientes de integración.

## Estructura principal

```text
src/main/java/ar/edu/uade/skillswap/
├── controllers/  # Contratos que consume la vista
├── domain/       # Modelo y reglas del dominio
├── application/  # Casos de uso, modelos y puertos
├── infrastructure/ # Repositorios y wiring en memoria
├── mock/         # Controller alternativo para pruebas de UI
├── model/        # Modelos usados por el Mock UI Kit
└── ui/           # Interfaz Java Swing
```

El `pom.xml` declara Hibernate, MySQL y Flyway, pero todavía no hay adaptadores JPA, migraciones ni conexiones a esos servicios. La aplicación usa exclusivamente repositorios en memoria.


## Integración en desarrollo (#33)

La rama `feature/33-integration-report` añade un controller de calificaciones y un selector de usuario de demostración en Swing (Mateo, Ana y Lucía), para ejercitar roles de comprador y proveedor. Esta selección no reemplaza una autenticación real.

**Estado de verificación:** estos cambios todavía requieren ejecución completa de `mvn clean test`, prueba visual del recorrido publicar → solicitar → aceptar → pagar → completar → calificar, integración del PR #61 y pruebas reales de MySQL/Flyway. No se dispone aún de un reporte de cobertura del 80% en Domain y Application.

El detalle de la validación pendiente está en [docs/issue33-integracion-y-validacion.md](docs/issue33-integracion-y-validacion.md). El modelo StarUML y su PDF se entregan por separado; no se modifican en esta rama.

## Flujo de trabajo

Los cambios se integran mediante pull requests hacia `main`. Consultá los issues del repositorio para el alcance y responsable de cada entrega.
