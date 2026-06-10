package com.vyv.rest.bdd.screenplay;

/**
 * Interfaz funcional para representar una tarea (Task) en el patrón Screenplay.
 */
@FunctionalInterface
public interface Tarea {
    void ejecutarComo(Actor actor);
}
