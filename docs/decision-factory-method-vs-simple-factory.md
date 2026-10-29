# Decisión de diseño #41 — Simple Factory frente a Factory Method

**Estado:** Factory Method evaluado y descartado por falta de una variación de creación concreta. Simple Factory implementado en la rama `feature/persistence-infrastructure` (PR #61, issue #25). Esta decisión se incorpora a la revisión transversal #35.

## 1. Problema real

SkillSwap debe enviar notificaciones de eventos de reservas y calificaciones a través de los canales elegidos por el destinatario: **EMAIL**, **PUSH** o **IN_APP**. Los casos de uso no deben conocer los clientes mock ni las clases concretas que envían cada notificación.

El contrato de salida es `NotificationGateway`. Los adaptadores concretos son `EmailNotificationAdapter`, `PushNotificationAdapter` e `InAppNotificationAdapter`. Los dos primeros traducen el mensaje interno a la interfaz de su cliente mock; el tercero guarda la notificación en el repositorio.

## 2. Diferencia conceptual

| Aspecto | Simple Factory | Factory Method (GoF) |
| --- | --- | --- |
| Mecanismo | Un objeto o función central recibe un criterio y decide qué implementación construir. | Una clase creadora declara un método de fábrica y sus subclases especializan qué producto concreto instanciar. |
| Punto de variación | El parámetro de selección y la lógica de la fábrica. | El tipo de creador concreto y su implementación del método de fábrica. |
| Caso de SkillSwap | `NotificationFactory.create(NotificationChannelType)` selecciona por canal. | No existe hoy una familia de creadores especializados con comportamientos de construcción distintos. |
| Consecuencia | Un `switch` centralizado que devuelve `NotificationGateway`. | Agregaría jerarquías y clases sin una necesidad demostrada. |

**Importante:** que `create` sea un método de instancia y no estático no lo convierte en Factory Method. La característica de Factory Method es la variación por subclases del creador, no el nombre del método ni si se usa `static`.

## 3. Evidencia de la Simple Factory

**Implementación:** `src/main/java/ar/edu/uade/skillswap/infrastructure/notification/NotificationFactory.java`.

```java
public NotificationGateway create(NotificationChannelType channel) {
    Objects.requireNonNull(channel, "Falta el canal de notificación");
    return switch (channel) {
        case EMAIL -> new EmailNotificationAdapter(emailClient);
        case PUSH -> new PushNotificationAdapter(pushClient);
        case IN_APP -> new InAppNotificationAdapter(notificationRepository);
    };
}
```

**Consumidores:** `NotificationEventObserver` y `RatingNotificationEventPublisher` solicitan un `NotificationGateway` a la fábrica por cada canal configurado en el perfil. Ambos operan mediante la interfaz interna; los servicios de Application no importan implementaciones concretas de email, push ni persistencia.

**Pruebas existentes:** `src/test/java/ar/edu/uade/skillswap/infrastructure/notification/NotificationFactoryTest.java` comprueba que EMAIL, PUSH e IN_APP devuelven los adaptadores esperados y que un canal nulo es rechazado. Las pruebas de `NotificationEventObserverTest` y `RatingNotificationEventPublisherTest` ejercitan la integración de preferencias, eventos y canales.

## 4. Evaluación de Factory Method

Se buscó una necesidad de **creación polimórfica por subclases**: por ejemplo, que diferentes tipos de proveedores deban construir familias o variantes de gateway mediante un método redefinido y que el consumidor pueda incorporar un creador nuevo sin modificar su código.

En el alcance actual no aparece esa necesidad:
- La elección está determinada por un **valor de canal**, no por el subtipo de un creador.
- Cada canal ya tiene un adaptador con el mismo contrato de salida, `NotificationGateway`.
- Los clientes mock y el repositorio son dependencias del constructor de la fábrica, no creadores intercambiables que deban especializarse.
- Introducir `NotificationCreator` más tres subclases creadoras repetiría la selección existente sin agregar una variación de negocio comprobable.

**Decisión:** no implementar Factory Method. Tampoco renombrar la Simple Factory para presentarla como Factory Method. Si en el futuro se necesita que diferentes proveedores controlen un proceso de creación con pasos específicos y extensibles por subclase, se reevalúa el patrón con ese caso de uso y pruebas de sustitución de creadores.

## 5. GRASP, SOLID y consecuencias

- **GRASP Protected Variations / Indirection:** el consumidor recibe `NotificationGateway`, que oculta detalles de los clientes mock.
- **GRASP High Cohesion:** la elección del canal se concentra en `NotificationFactory`; la conversión del mensaje queda en cada Adapter.
- **SRP:** la fábrica selecciona/crea; los adapters envían o persisten; Application coordina casos de uso.
- **DIP:** Application depende de `NotificationGateway`, no de `MockEmailClient` ni `MockPushClient`.
- **OCP (compromiso explícito):** incorporar un canal nuevo hoy requiere actualizar el `switch` de la Simple Factory y el enum de canales, pero no los casos de uso. Factory Method podría cambiar ese punto de extensión si surgiera una necesidad real, pero no se justifica solo para evitar este `switch`.

## 6. Alcance de la validación

No se agregan pruebas de Factory Method porque **no se implementa**. Se reutiliza la evidencia de Simple Factory de la #25. Este cambio es **solo documental**: no altera el comportamiento, el código Java ni el UML editable. La verificación de ejecución de las pruebas corresponde al PR #61; esta decisión no afirma haber ejecutado Maven nuevamente.

**Trazabilidad:** #25 (Adapter y Simple Factory), #35 (auditoría de patrones) y #41 (decisión de Factory Method). El informe general puede citar este documento para la defensa oral.
