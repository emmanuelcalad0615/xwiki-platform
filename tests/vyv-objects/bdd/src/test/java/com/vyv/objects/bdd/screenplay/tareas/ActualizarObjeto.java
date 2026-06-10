package com.vyv.objects.bdd.screenplay.tareas;

import com.vyv.objects.bdd.screenplay.Tarea;
import com.vyv.objects.bdd.screenplay.habilidades.UsarLaApiDeObjetos;

/** Tarea Screenplay: actualizar un objeto de la pagina via PUT. */
public final class ActualizarObjeto
{
    private ActualizarObjeto()
    {
    }

    public static Tarea numerado(String clase, int numero)
    {
        return actor -> actor.usando(UsarLaApiDeObjetos.class).actualizarObjeto(clase, numero);
    }
}
