package com.vyv.rest.bdd.screenplay;

import java.util.HashMap;
import java.util.Map;

/**
 * Actor del patrón Screenplay. Ejecuta Tareas y responde Preguntas usando sus habilidades.
 */
public final class Actor {
    private final String nombre;
    private final Map<Class<?>, Object> habilidades = new HashMap<>();

    private Actor(String nombre) {
        this.nombre = nombre;
    }

    public static Actor llamado(String nombre) {
        return new Actor(nombre);
    }

    public String getNombre() {
        return this.nombre;
    }

    public <T> Actor puede(T habilidad) {
        this.habilidades.put(habilidad.getClass(), habilidad);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T usando(Class<T> tipo) {
        Object habilidad = this.habilidades.get(tipo);
        if (habilidad == null) {
            throw new IllegalStateException(this.nombre + " no tiene la habilidad " + tipo.getSimpleName());
        }
        return (T) habilidad;
    }

    public Actor intenta(Tarea... tareas) {
        for (Tarea tarea : tareas) {
            tarea.ejecutarComo(this);
        }
        return this;
    }

    public <T> T responde(Pregunta<T> pregunta) {
        return pregunta.respondidaPor(this);
    }
}
