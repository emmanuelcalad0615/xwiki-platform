package com.vyv.rest.bdd.screenplay.tareas;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Tarea;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeComentarios;

public class CrearComentario implements Tarea {

    private final String texto;

    public CrearComentario(String texto) {
        this.texto = texto;
    }

    public static CrearComentario conTexto(String texto) {
        return new CrearComentario(texto);
    }

    @Override
    public void ejecutarComo(Actor actor) {
        actor.usando(UsarLaApiDeComentarios.class).crearComentario(this.texto);
    }
}
