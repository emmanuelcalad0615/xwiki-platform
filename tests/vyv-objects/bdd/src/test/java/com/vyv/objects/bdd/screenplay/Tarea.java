package com.vyv.objects.bdd.screenplay;

/**
 * Tarea del patron Screenplay (Task): una accion de negocio que el actor
 * intenta realizar sobre el sistema.
 */
@FunctionalInterface
public interface Tarea
{
    void ejecutarComo(Actor actor);
}
