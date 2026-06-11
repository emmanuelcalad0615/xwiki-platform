package com.vyv.rest.bdd.screenplay.tareas;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Tarea;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeComentarios;

public class ConsultarComentarios implements Tarea {

    public static ConsultarComentarios deLaPagina() {
        return new ConsultarComentarios();
    }

    @Override
    public void ejecutarComo(Actor actor) {
        actor.usando(UsarLaApiDeComentarios.class).consultarComentarios();
    }
}
