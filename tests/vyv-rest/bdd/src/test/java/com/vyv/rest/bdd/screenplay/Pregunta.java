package com.vyv.rest.bdd.screenplay;

/**
 * Interfaz funcional para representar una pregunta (Question) en el patrón Screenplay.
 */
@FunctionalInterface
public interface Pregunta<T> {
    T respondidaPor(Actor actor);
}
