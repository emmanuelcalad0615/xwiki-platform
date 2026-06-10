package com.vyv.rest.bdd.screenplay.preguntas;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Pregunta;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeEtiquetas;

public class ElCodigoDeEstado implements Pregunta<Integer> {

    public static ElCodigoDeEstado retornado() {
        return new ElCodigoDeEstado();
    }

    @Override
    public Integer respondidaPor(Actor actor) {
        UsarLaApiDeEtiquetas api = actor.usando(UsarLaApiDeEtiquetas.class);
        return api.getCodigoHttpRespuesta();
    }
}
