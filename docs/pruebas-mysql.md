# Validación de persistencia contra MySQL

## Alcance

`MySqlPersistenceIntegrationTest` usa MySQL real, Flyway y la unidad JPA de la aplicación con `hibernate.hbm2ddl.auto=validate`. Hibernate no crea las tablas. El perfil `mysql-it` habilita estas pruebas; sin ese perfil se omiten explícitamente y continúa la suite habitual.

Cada prueba crea una base aleatoria `skillswap_it_<UUID>` y la elimina al finalizar. Se exige un usuario de un servidor de pruebas con permisos para crear y eliminar esas bases. La URL debe apuntar al servidor sin seleccionar una base existente. No usar credenciales ni servidores de producción.

Se verifican:

- Migración de un esquema vacío y actualización de V1 con una contratación previa: V2 mantiene `client_message` nulo y el dominio lo normaliza a cadena vacía. Flyway valida checksums y una nueva ejecución no reaplica migraciones.
- Guardado, recuperación y actualización de `clientMessage` con los repositorios reales. Migración del ledger existente y conservación del orden de inserción aunque las fechas coincidan y los IDs se ordenen al revés.
- Rollback después de enviar SQL con `flush`: una aceptación fallida no consume cupos; un pago fallido no modifica saldo, ledger ni estado. Los eventos registrados con `afterCommit` no se publican al revertir y el contexto puede usarse después.
- Dos aceptaciones simultáneas del último cupo: una gana y la otra recibe un rechazo de negocio. Se comprueba la reserva y el estado persistido. Tres repeticiones.
- Dos pagos simultáneos de la misma contratación: un solo débito/crédito, un solo evento y un solo pago confirmado. Tres repeticiones.
- Pagos de contrataciones distintas sobre la misma billetera con fondos para una sola: no hay doble gasto.
- Filtros por texto, categoría, modalidad, precio y reputación; conteo total, orden por precio, páginas sucesivas sin duplicados y página fuera de rango. Desempate por ID cuando precio y fecha son idénticos.
- Orden por reputación del autor antes de la fecha, con paginación y filtro mínimo de reputación.
- Registro, consulta, actualización de habilidades/intereses, preferencias y disponibilidad, baja lógica y reactivación de perfiles.
- Calificación persistida, actualización de reputación, rollback conjunto y unicidad por contratación/autor, comprobada en Application y directamente en MySQL. Las calificaciones duplicadas simultáneas también se verifican tres veces.
- Bandejas de notificaciones por destinatario, lectura individual y masiva, permisos y rollback.
- Reinicio del punto de composición JPA y conexión a `WiredUiControllers`: se conservan perfiles, saldo/ledger, contratación, calificación/reputación y lectura de la bienvenida. No se duplican publicaciones pausadas ni se resetean datos iniciales. La fábrica JPA se cierra correctamente.

Las carreras usan conexiones y transacciones independientes en dos hilos, coordinados por barreras con tiempos máximos. Un error SQL, timeout o deadlock no cuenta como un rechazo de negocio válido. Estos escenarios no sustituyen pruebas de carga ni certifican todos los intercalados posibles.

## Correcciones encontradas al ejecutar contra MySQL

- La unidad JPA de producción registra explícitamente sus entidades, para no depender del escaneo automático en Java SE. La prueba de H2 anterior solo usaba una unidad reducida de test.
- Se configura `READ_COMMITTED` en las conexiones Hibernate y se bloquean/refrescan las filas de disponibilidad antes de convertirlas a dominio. Así, después de esperar un bloqueo se leen los cupos actuales. Con el snapshot anterior de `REPEATABLE_READ`, la prueba permitió dos aceptaciones del último cupo. Las billeteras mantienen el orden de adquisición de bloqueos que ya define Application. La diferencia entre snapshots está documentada en el [manual de MySQL](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html).
- V3 incorpora `movement_index` único por billetera y conserva microsegundos en el ledger. Los mappers/repositorios conservan la secuencia de negocio en vez de ordenar por UUID cuando las fechas coinciden. V1 y V2 se mantienen intactas para conservar sus checksums. Los movimientos antiguos se numeran según el orden de lectura anterior (`occurred_at`, `id`); una billetera que ya fuera inconsistente necesita auditoría por separado, porque la precisión perdida no se puede recuperar mediante una migración.
- La búsqueda incorpora el ID como último criterio de orden para estabilizar los límites de página cuando otros criterios empatan.

Las reglas de estados, participantes, saldo y capacidad continúan en Domain/Application. Las correcciones se limitan a Infrastructure, configuración, migración y pruebas.

El modo `--persistence=jpa` ejecuta Flyway y compone todos los repositorios JPA con un único `JpaTransactionManager` y `EntityManagerFactory`. La composición de servicios, Observer y Simple Factory es compartida con el modo en memoria. Los proveedores de email/push e identidad continúan siendo mock. El [README](../README.md) detalla cómo iniciar la interfaz con la base configurada.

## Ejecución local en PowerShell

Con Docker iniciado y JDK 17/Maven disponibles:

```powershell
docker run --detach --name skillswap-mysql-it --publish 127.0.0.1:33361:3306 --env MYSQL_ROOT_PASSWORD=skillswap-it-only --env MYSQL_ROOT_HOST=% mysql:8.4 --default-time-zone=+00:00
```

Esperar a que esta comprobación informe `mysqld is alive`:

```powershell
docker exec skillswap-mysql-it mysqladmin ping -h 127.0.0.1 -pskillswap-it-only
```

Desde la raíz del proyecto:

```powershell
$env:SKILLSWAP_MYSQL_TEST_URL = 'jdbc:mysql://127.0.0.1:33361/?allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC'
$env:SKILLSWAP_MYSQL_TEST_USER = 'root'
$env:SKILLSWAP_MYSQL_TEST_PASSWORD = 'skillswap-it-only'
mvn -Pmysql-it clean test
```

La contraseña publicada es solo para este contenedor efímero, cuyo puerto se limita a localhost. Al terminar, detener y eliminar únicamente ese contenedor:

```powershell
docker stop skillswap-mysql-it
docker rm skillswap-mysql-it
```

## Integración continua

`.github/workflows/mysql-integration.yml` crea un servicio MySQL separado y ejecuta `mvn -Pmysql-it clean test` en PR hacia `main`. Los resultados locales y los checks remotos se reportan por separado; una ejecución pendiente no equivale a una aprobación. Este cambio no fusiona PR ni modifica UML.

## Resultado registrado el 02/11/2026

- Entorno local: JDK 17, Maven 3.9.9, MySQL Community Server 8.4.13 en Docker, conexiones `READ_COMMITTED`.
- Imagen: `mysql:8.4`, digest `sha256:a0225d1ea6631c8bc2385dc4c2faba17bbf1834ecf369e1659a9a382bd8d5dd7`.
- `mvn -Pmysql-it clean test`: **131 pruebas, 0 fallos, 0 errores y 0 omitidas**. Finalizó a las 11:51 de Argentina.
- Incluye **14 ejecuciones** de `MySqlPersistenceIntegrationTest` (las carreras de último cupo y doble pago se repiten tres veces cada una), además de las 117 pruebas anteriores.
- Flyway aplica V1, V2 y V3; Hibernate valida el esquema migrado. También se prueba actualizar datos existentes desde V1 y volver a ejecutar las migraciones sin cambios.
- `git diff --check`: limpio.

Ese resultado inicial cubría los escenarios existentes hasta ese cambio. La ampliación siguiente agrega los repositorios restantes y el modo JPA ejecutable.

## Ampliación y conexión de la aplicación (02/11/2026)

- `mvn -Pmysql-it clean test`: **140 pruebas, 0 fallos, 0 errores y 0 omitidas**; finalizó a las 12:12 de Argentina en el mismo entorno MySQL 8.4.13/JDK 17.
- Incluye **23 ejecuciones de integración**: las 14 anteriores más perfiles, reputación/ordenamiento, calificaciones/rollback/unicidad/concurrencia, notificaciones y reinicio del punto de composición JPA.
- El modo `--persistence=jpa` se inició desde `SkillSwapApplication` con las variables `SKILLSWAP_DB_*` y generó una captura de Swing. Se repitió el arranque sobre la misma base temporal. Flyway no reaplicó migraciones ni se duplicaron los datos iniciales.
- Las tarjetas muestran el nombre y reputación del autor mediante un DTO público de Discovery; no se generan puntuaciones ficticias desde el controller.
- El reinicio automatizado cierra y abre una fábrica JPA nueva, verifica datos persistidos y ejercita `WiredUiControllers` sobre la composición real. El cierre libera la fábrica y sus conexiones.
- Para una captura mediante `exec:java` puede usarse `-Dcom.mysql.cj.disableAbandonedConnectionCleanup=true` como propiedad de Maven: evita que el hilo de limpieza del driver sobreviva al grupo de hilos del plugin después de cerrar las conexiones. Esta opción no se requiere en las pruebas de Surefire.
- `git diff --check`: limpio.

Pendientes fuera de este alcance: recorrido visual completo de todas las pantallas, integración del selector/calificación de #65, medición de cobertura y carga de producción. Las pruebas no certifican todos los intercalados posibles ni entrega durable de notificaciones externas.
