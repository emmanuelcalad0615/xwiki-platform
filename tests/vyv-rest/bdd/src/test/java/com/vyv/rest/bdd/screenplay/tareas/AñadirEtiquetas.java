package com.vyv.rest.bdd.screenplay.tareas;

import java.util.List;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Tarea;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeEtiquetas;

public class AñadirEtiquetas implements Tarea {

    private final List<String> etiquetas;

    public AñadirEtiquetas(List<String> etiquetas) {
        this.etiquetas = etiquetas;
    }

    public static AñadirEtiquetas aLaPagina(List<String> etiquetas) {
        return new AñadirEtiquetas(etiquetas);
    }

    @Override
    public void ejecutarComo(Actor actor) {
        UsarLaApiDeEtiquetas api = actor.usando(UsarLaApiDeEtiquetas.class);
        api.anadirEtiquetas(this.etiquetas);
    }
}
