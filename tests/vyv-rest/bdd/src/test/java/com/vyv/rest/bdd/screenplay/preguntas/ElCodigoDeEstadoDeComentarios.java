package com.vyv.rest.bdd.screenplay.preguntas;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Pregunta;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeComentarios;

public class ElCodigoDeEstadoDeComentarios implements Pregunta<Integer> {

    public static ElCodigoDeEstadoDeComentarios retornado() {
        return new ElCodigoDeEstadoDeComentarios();
    }

    @Override
    public Integer respondidaPor(Actor actor) {
        return actor.usando(UsarLaApiDeComentarios.class).getCodigoHttpRespuesta();
    }
}
