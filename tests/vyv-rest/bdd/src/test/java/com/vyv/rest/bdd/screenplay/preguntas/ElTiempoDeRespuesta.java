package com.vyv.rest.bdd.screenplay.preguntas;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Pregunta;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeEtiquetas;

public class ElTiempoDeRespuesta implements Pregunta<Long> {

    public static ElTiempoDeRespuesta deLaApiEnMilisegundos() {
        return new ElTiempoDeRespuesta();
    }

    @Override
    public Long respondidaPor(Actor actor) {
        UsarLaApiDeEtiquetas api = actor.usando(UsarLaApiDeEtiquetas.class);
        return api.getTiempoRespuestaMs();
    }
}
