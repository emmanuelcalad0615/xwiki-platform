package com.vyv.objects.bdd.screenplay.preguntas;

import com.vyv.objects.bdd.screenplay.Pregunta;
import com.vyv.objects.bdd.screenplay.habilidades.UsarLaApiDeObjetos;

/** Preguntas Screenplay sobre el resultado de la ultima operacion del actor. */
public final class Respuestas
{
    private Respuestas()
    {
    }

    /** Codigo HTTP con el que fallo la ultima operacion (401, 404, ...). */
    public static Pregunta<Integer> elCodigoHttpDeError()
    {
        return actor -> actor.usando(UsarLaApiDeObjetos.class).getCodigoHttpDeError();
    }

    /** Codigo HTTP de la respuesta exitosa (p. ej. 202 ACCEPTED). */
    public static Pregunta<Integer> elCodigoHttpDeRespuesta()
    {
        return actor -> actor.usando(UsarLaApiDeObjetos.class).getCodigoHttpDeRespuesta();
    }

    /** El objeto REST recibido en la consulta. */
    public static Pregunta<org.xwiki.rest.model.jaxb.Object> elObjetoRecibido()
    {
        return actor -> actor.usando(UsarLaApiDeObjetos.class).getObjetoRecibido();
    }

    /** Si la ultima operacion termino en error interno del wiki (XWikiRestException). */
    public static Pregunta<Boolean> siHuboErrorInternoDelWiki()
    {
        return actor -> actor.usando(UsarLaApiDeObjetos.class).huboErrorInternoDelWiki();
    }
}
