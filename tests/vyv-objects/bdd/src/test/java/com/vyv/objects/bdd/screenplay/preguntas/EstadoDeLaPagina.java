package com.vyv.objects.bdd.screenplay.preguntas;

import com.vyv.objects.bdd.screenplay.Pregunta;
import com.vyv.objects.bdd.screenplay.habilidades.UsarLaApiDeObjetos;

/** Preguntas Screenplay sobre el estado de la pagina simulada. */
public final class EstadoDeLaPagina
{
    private EstadoDeLaPagina()
    {
    }

    /** Cuantas veces se guardo la pagina durante el escenario. */
    public static Pregunta<Integer> lasVecesQueSeGuardo()
    {
        return actor -> actor.usando(UsarLaApiDeObjetos.class).getVecesGuardada();
    }

    /** Si el objeto (clase, numero) sigue existiendo en la pagina. */
    public static Pregunta<Boolean> siTieneElObjeto(String clase, int numero)
    {
        return actor -> actor.usando(UsarLaApiDeObjetos.class).tieneObjeto(clase, numero);
    }

    /** Cuantas conversiones REST->objeto se aplicaron (cambios recibidos). */
    public static Pregunta<Integer> losCambiosAplicados()
    {
        return actor -> actor.usando(UsarLaApiDeObjetos.class).getConversiones();
    }
}
