package com.vyv.objects.bdd.screenplay.tareas;

import com.vyv.objects.bdd.screenplay.Tarea;
import com.vyv.objects.bdd.screenplay.habilidades.UsarLaApiDeObjetos;

/** Tarea Screenplay: consultar un objeto de la pagina via GET. */
public final class ConsultarObjeto
{
    private ConsultarObjeto()
    {
    }

    public static Tarea numerado(String clase, int numero)
    {
        return actor -> actor.usando(UsarLaApiDeObjetos.class).consultarObjeto(clase, numero);
    }
}
