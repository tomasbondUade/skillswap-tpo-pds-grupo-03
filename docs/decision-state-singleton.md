# Decisión de diseño: State y Singleton en contrataciones

## State

`Booking` delega las transiciones del ciclo de vida a `BookingState`. Las implementaciones de `BookingStates` permiten solo las operaciones válidas para cada etapa: por ejemplo, `Pending` acepta, rechaza o cancela; `Accepted` permite pagar o cancelar; y `InProgress` permite completar. Las transiciones no disponibles heredan la excepción de `BookingState`, evitando condicionales crecientes dentro del agregado.

`Booking` sigue siendo el dueño de los datos variables de una contratación: participantes, precio, pago, reprogramación e historial. Al cambiar de estado, agrega un `StatusHistoryEntry` inmutable y reemplaza su referencia al estado. El historial se devuelve como copia inmodificable.

## Singleton por estado

Cada implementación concreta de `BookingState` es un `enum` privado con una única constante. Esto aplica Singleton solo a los objetos de estado, porque no contienen información propia de una contratación y su comportamiento es el mismo para todas. En Java, los enums garantizan una instancia única por constante y son seguros frente a concurrencia y serialización sin requerir un Singleton manual.

La instancia compartida no guarda actor, fecha, pago, participante ni historial. Esos datos pertenecen a cada `Booking`, por lo que una transición en una contratación no altera otra. La prueba `bookingStateSingletonsAreStatelessAndDoNotShareBookingData` verifica identidad de la instancia `Pending` y que avanzar una contratación no cambia el estado ni el historial de otra.

No se aplica Singleton a `Booking`: cada solicitud representa una contratación independiente y debe mantener su identidad y estado propios.

## Consecuencias

- Las reglas de transición quedan junto a los estados concretos y se prueban desde las operaciones del agregado.
- Compartir enums evita instanciar objetos de estado equivalentes y garantiza una instancia por etapa.
- Para agregar una etapa nueva deben actualizarse el protocolo, la transición de origen, las pruebas y el UML.
- La decisión se limita a estados sin datos propios; si una etapa necesitara guardar datos individuales, esos datos deberían ir en `Booking` u otro objeto por contratación, no en el enum compartido.
