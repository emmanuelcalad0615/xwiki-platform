package com.vyv.objects.bdd.screenplay;

/**
 * Pregunta del patron Screenplay (Question): consulta sobre el estado
 * observable del sistema, respondida desde la perspectiva del actor.
 *
 * @param <T> tipo de la respuesta
 */
@FunctionalInterface
public interface Pregunta<T>
{
    T respondidaPor(Actor actor);
}
