package com.vyv.rest.bdd.screenplay.tareas;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Tarea;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeComentarios;

public class ObtenerComentario implements Tarea {

    private final int id;

    public ObtenerComentario(int id) {
        this.id = id;
    }

    public static ObtenerComentario conId(int id) {
        return new ObtenerComentario(id);
    }

    @Override
    public void ejecutarComo(Actor actor) {
        actor.usando(UsarLaApiDeComentarios.class).obtenerComentario(this.id);
    }
}
