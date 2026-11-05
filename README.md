# SkillSwap

SkillSwap es un marketplace académico para ofrecer y contratar mentorías. Este repositorio corresponde al Trabajo Práctico de Proceso de Desarrollo de Software.

## Estado actual

La aplicación ejecutable es una demo de escritorio en Java Swing. La vista usa controllers que conectan perfiles, publicaciones, búsqueda, contrataciones, billetera y notificaciones con servicios de Application. El modo predeterminado usa memoria; `--persistence=jpa` conecta esos mismos servicios a MySQL mediante repositorios JPA. `MockSkillSwapControllers` permanece disponible para pruebas y recorridos aislados.

El modo en memoria no requiere MySQL y pierde los datos al cerrar. El modo JPA conserva los datos entre ejecuciones; Flyway aplica las migraciones antes de que Hibernate valide el esquema. Ambos modos usan identidades y proveedores de demostración y SkillCoins ficticios; no hay autenticación ni pasarela de pago reales.

## Requisitos

- JDK 17 o superior.
- Apache Maven 3.9 o superior.
- Git.

No se necesitan variables de entorno ni credenciales para ejecutar el modo en memoria. El modo JPA requiere una base MySQL 8.4 y las variables indicadas abajo.

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

### Ejecutar con persistencia MySQL

Crear primero una base vacía dedicada a la demo y un usuario con permisos sobre ella, incluidos los necesarios para migraciones y tablas temporales. En PowerShell, configurar los datos de esa base:

```powershell
$env:SKILLSWAP_DB_URL = 'jdbc:mysql://127.0.0.1:3306/skillswap?connectionTimeZone=UTC'
$env:SKILLSWAP_DB_USER = 'skillswap'
$env:SKILLSWAP_DB_PASSWORD = '<contraseña del usuario de la demo>'
mvn exec:java '-Dexec.args=--persistence=jpa'
```

La ventana indica **SkillSwap · MySQL**. En la primera ejecución se crean perfiles, billeteras y publicaciones de muestra. Al reiniciar no se reemplazan perfiles, no se reinician saldos ni se duplican la bienvenida o las publicaciones, incluso si estas fueron pausadas. Al cerrar se liberan las conexiones JPA. En esta rama el selector demo permite alternar entre Mateo, Ana y Lucía tanto en memoria como con JPA; no reemplaza autenticación real.

## Recorrido de demostración

El guion detallado está en [`docs/guion-demo.md`](docs/guion-demo.md). Describe los flujos conectados, el selector de identidad de demo y las limitaciones de la persistencia en memoria.

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

Esta rama incorpora adaptadores JPA y migraciones Flyway V1–V3. La suite de integración utiliza la unidad JPA y el punto de composición de la aplicación contra MySQL real; los pasos para ejecutarla, sus escenarios y límites están en [Pruebas de MySQL](docs/pruebas-mysql.md). Se habilita con `mvn -Pmysql-it clean test` y requiere las variables de conexión de pruebas allí documentadas. Sin ese perfil, las pruebas de MySQL se omiten explícitamente.


## Integración en desarrollo (#33)

La rama `feature/33-integration-report` añade un controller de calificaciones y un selector de usuario de demostración en Swing (Mateo, Ana y Lucía), para ejercitar roles de comprador y proveedor. Esta selección no reemplaza una autenticación real.

**Estado de verificación:** la revisión del 05/11/2026 incorpora la persistencia y la búsqueda actualmente integradas en `main` y resuelve los conflictos con la base de #63. Los resultados históricos de 124 pruebas corresponden a una versión anterior; el registro actualizado está en [docs/issue33-integracion-y-validacion.md](docs/issue33-integracion-y-validacion.md). Siguen pendientes el recorrido visual completo y el reporte de cobertura del 80% en Domain/Application. La rama permanece en Draft.

El detalle de la validación pendiente está en [docs/issue33-integracion-y-validacion.md](docs/issue33-integracion-y-validacion.md). El modelo StarUML y su PDF se entregan por separado; no se modifican en esta rama.

## Flujo de trabajo

Los cambios se integran mediante pull requests hacia `main`. Consultá los issues del repositorio para el alcance y responsable de cada entrega.
