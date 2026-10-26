# Decisión de diseño: Builder para publicaciones

## Problema

Crear una publicación requiere varios datos obligatorios (autor, título, descripción, categoría, modalidad, duración, cupo y precio), mientras que las etiquetas y franjas de disponibilidad pueden agregarse en momentos distintos del ciclo de vida del borrador. Un constructor posicional extenso es difícil de leer y facilita intercambiar argumentos del mismo tipo.

## Decisión

`Publication.builder(authorUserId)` expone una construcción fluida, y `PublicationBuilder` conserva los valores hasta que `build()` comprueba la presencia de los datos requeridos. Las etiquetas y franjas son opcionales para permitir un borrador todavía no publicable.

El Builder no reemplaza las reglas del agregado: `build()` crea mediante `Publication.draft(...)` y agrega las franjas con `Publication.addAvailability(...)`. De esa manera `Publication` sigue validando los valores positivos, la duración mínima de cada franja, el cupo, la ausencia de solapamientos y demás invariantes. Un borrador sin franjas se puede construir, pero la entidad impide publicarlo hasta que tenga disponibilidad válida.

## Por qué Builder aporta en este caso

La creación tiene varios pasos opcionales, exige ocho datos obligatorios de tipos distintos y necesita validar una colección variable de franjas antes de poder publicar. Builder hace explícito qué dato se configura en cada llamada y en qué momento se completa la construcción. No se agrega una jerarquía de builders ni un director: una sola clase resuelve este proceso concreto.

## Consecuencias y responsabilidades

- `PublicationBuilder` organiza la configuración, detecta datos obligatorios ausentes y delega la creación del agregado.
- `Publication` mantiene las reglas de negocio y el estado mutable controlado de un borrador.
- `AvailabilitySlot` valida el intervalo y la capacidad propia de cada franja.
- El Builder pertenece al paquete de dominio y no depende de DTOs, persistencia, frameworks ni APIs externas.
- Las pruebas cubren construcción con y sin opcionales, campos obligatorios, límites y franjas inválidas.
