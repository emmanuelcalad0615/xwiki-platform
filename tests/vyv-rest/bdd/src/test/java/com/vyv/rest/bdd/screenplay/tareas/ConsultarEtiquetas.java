package com.vyv.rest.bdd.screenplay.tareas;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Tarea;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeEtiquetas;

public class ConsultarEtiquetas implements Tarea {

    public static ConsultarEtiquetas deLaPagina() {
        return new ConsultarEtiquetas();
    }

    @Override
    public void ejecutarComo(Actor actor) {
        UsarLaApiDeEtiquetas api = actor.usando(UsarLaApiDeEtiquetas.class);
        api.consultarEtiquetas();
    }
}
