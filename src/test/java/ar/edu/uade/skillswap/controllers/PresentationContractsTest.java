package ar.edu.uade.skillswap.controllers;

import ar.edu.uade.skillswap.application.model.*;
import ar.edu.uade.skillswap.model.Models;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PresentationContractsTest {
    @Test
    void losControllersDePerfilYPublicacionesDependenDePuertosSinDetallesDePersistencia() {
        for (Class<?> controller : List.of(PerfilUiController.class, PublicacionUiController.class,
                ContratacionUiController.class)) {
            for (var field : controller.getDeclaredFields()) {
                String packageName = field.getType().getPackageName();
                assertFalse(packageName.startsWith("ar.edu.uade.skillswap.infrastructure"));
                assertFalse(packageName.startsWith("jakarta.persistence"));
            }
        }
    }

    @Test
    void losDtosSoloContienenDatosSimplesYOtrosDtos() {
        for (Class<?> container : List.of(Models.class, BookingData.class, DiscoveryData.class,
                ProfileData.class, PublicationData.class, RatingData.class, WalletData.class,
                NotificationData.class)) {
            for (Class<?> nested : container.getDeclaredClasses()) {
                if (nested.isRecord()) assertDataType(nested, new HashSet<>());
            }
        }
    }

    @Test
    void losContratosDeControllersNoRecibenNiDevuelvenEntidades() {
        for (Class<?> controller : UiControllers.class.getDeclaredClasses()) {
            for (var method : controller.getDeclaredMethods()) {
                assertDataType(method.getGenericReturnType(), new HashSet<>());
                for (Type parameter : method.getGenericParameterTypes()) {
                    assertDataType(parameter, new HashSet<>());
                }
            }
        }
    }

    @Test
    void elPerfilNoComparteSuListaMutableConLaPantalla() {
        var skills = new ArrayList<>(List.of("Java"));
        var profile = new Models.Perfil("u1", "Julieta", "Mentora", "Bio", skills, 4, 2, 1);
        skills.clear();
        assertEquals(List.of("Java"), profile.habilidades());
        assertThrows(UnsupportedOperationException.class, () -> profile.habilidades().add("SQL"));
    }

    private void assertDataType(Type type, Set<Type> visited) {
        if (!visited.add(type)) return;
        if (type instanceof ParameterizedType parameterized) {
            assertTrue(parameterized.getRawType() == List.class || parameterized.getRawType() == Set.class,
                    "Colección no prevista en el contrato: " + type);
            for (Type argument : parameterized.getActualTypeArguments()) assertDataType(argument, visited);
            return;
        }
        assertInstanceOf(Class.class, type);
        Class<?> value = (Class<?>) type;
        if (value.isPrimitive() || value == String.class || value == LocalDateTime.class
                || value == Integer.class || value == Double.class || value == Boolean.class
                || value.isEnum()) return;
        assertTrue(value.isRecord(), "Un DTO o controller expone una entidad: " + value.getName());
        assertFalse(value.getPackageName().startsWith("ar.edu.uade.skillswap.domain"));
        assertFalse(value.getPackageName().startsWith("ar.edu.uade.skillswap.infrastructure"));
        for (var component : value.getRecordComponents()) {
            assertDataType(component.getGenericType(), visited);
        }
    }
}
