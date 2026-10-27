# SkillSwap

SkillSwap es un marketplace académico para ofrecer y contratar mentorías. Este repositorio corresponde al Trabajo Práctico de Proceso de Desarrollo de Software.

## Estado actual

La aplicación ejecutable es un Mock UI Kit de escritorio en Java Swing. La presentación consume interfaces de controller y, en esta versión, `MockSkillSwapControllers` responde con datos en memoria. El modelo de dominio contiene reglas para perfiles, publicaciones, contrataciones, billeteras, calificaciones y sus estados.

La interfaz de demostración no persiste datos y no requiere una base MySQL configurada. Al cerrar y volver a abrir la aplicación, los datos de prueba vuelven a su estado inicial. La integración completa entre Presentation, Application e Infrastructure se realiza en los issues de implementación del equipo.

## Requisitos

- JDK 17 o superior.
- Apache Maven 3.9 o superior.
- Git.

No se necesitan variables de entorno ni credenciales para ejecutar el Mock UI Kit.

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

El guion detallado está en [`docs/guion-demo.md`](docs/guion-demo.md). Resume cómo crear y publicar un borrador, explorar mentorías, generar una solicitud y avanzar sus estados desde la interfaz actual. También distingue las interacciones simuladas de las que quedan pendientes de integración.

## Diseño y defensa

- [Modelo editable de clases en StarUML](docs/modelo-skill-swap.mdj), con diagramas de Domain y Application.
- [Informe de decisiones de diseño](docs/informe-diseno.md) y [versión PDF](output/pdf/informe-diseno-skillswap.pdf).
- [Auditoría de patrones, GRASP y SOLID](docs/auditoria-patrones-y-grasp.md), con estado verificado en `main` y pendientes de integración.

## Estructura principal

```text
src/main/java/ar/edu/uade/skillswap/
├── controllers/  # Contratos que consume la vista
├── domain/       # Modelo y reglas del dominio
├── mock/         # Implementación en memoria para la demo
├── model/        # Modelos usados por el Mock UI Kit
└── ui/           # Interfaz Java Swing
```

El `pom.xml` declara Hibernate, MySQL y Flyway como dependencias previstas para la integración. La aplicación de demostración actual usa el mock y no accede a esos servicios.

## Flujo de trabajo

Los cambios se integran mediante pull requests hacia `main`. Consultá los issues del repositorio para el alcance y responsable de cada entrega.
