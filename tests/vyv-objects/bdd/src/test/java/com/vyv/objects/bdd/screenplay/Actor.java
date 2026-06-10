package com.vyv.objects.bdd.screenplay;

import java.util.HashMap;
import java.util.Map;

/**
 * Actor del patron Screenplay: representa a la persona que interactua con el
 * sistema. Ejecuta {@link Tarea}s y responde {@link Pregunta}s usando sus
 * habilidades registradas.
 */
public final class Actor
{
    private final String nombre;

    private final Map<Class<?>, Object> habilidades = new HashMap<>();

    private Actor(String nombre)
    {
        this.nombre = nombre;
    }

    public static Actor llamado(String nombre)
    {
        return new Actor(nombre);
    }

    public String getNombre()
    {
        return this.nombre;
    }

    /** Registra una habilidad (Ability) del actor. */
    public <T> Actor puede(T habilidad)
    {
        this.habilidades.put(habilidad.getClass(), habilidad);
        return this;
    }

    /** Recupera una habilidad por su tipo. */
    @SuppressWarnings("unchecked")
    public <T> T usando(Class<T> tipo)
    {
        Object habilidad = this.habilidades.get(tipo);
        if (habilidad == null) {
            throw new IllegalStateException(
                this.nombre + " no tiene la habilidad " + tipo.getSimpleName());
        }
        return (T) habilidad;
    }

    /** Ejecuta una o varias tareas (Tasks) en orden. */
    public Actor intenta(Tarea... tareas)
    {
        for (Tarea tarea : tareas) {
            tarea.ejecutarComo(this);
        }
        return this;
    }

    /** Responde una pregunta (Question) sobre el estado del sistema. */
    public <T> T responde(Pregunta<T> pregunta)
    {
        return pregunta.respondidaPor(this);
    }
}
