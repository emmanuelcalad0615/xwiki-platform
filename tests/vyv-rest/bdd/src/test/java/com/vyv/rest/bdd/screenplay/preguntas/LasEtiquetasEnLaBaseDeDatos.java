package com.vyv.rest.bdd.screenplay.preguntas;

import java.util.List;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Pregunta;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeEtiquetas;

public class LasEtiquetasEnLaBaseDeDatos implements Pregunta<List<String>> {

    public static LasEtiquetasEnLaBaseDeDatos delSistema() {
        return new LasEtiquetasEnLaBaseDeDatos();
    }

    @Override
    public List<String> respondidaPor(Actor actor) {
        UsarLaApiDeEtiquetas api = actor.usando(UsarLaApiDeEtiquetas.class);
        return api.getEtiquetasGuardadasEnBaseDeDatosFake();
    }
}
