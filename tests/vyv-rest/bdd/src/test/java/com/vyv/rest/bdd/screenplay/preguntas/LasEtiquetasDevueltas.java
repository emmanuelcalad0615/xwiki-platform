package com.vyv.rest.bdd.screenplay.preguntas;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.xwiki.rest.model.jaxb.Tags;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Pregunta;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeEtiquetas;

public class LasEtiquetasDevueltas implements Pregunta<List<String>> {

    public static LasEtiquetasDevueltas porLaApi() {
        return new LasEtiquetasDevueltas();
    }

    @Override
    public List<String> respondidaPor(Actor actor) {
        UsarLaApiDeEtiquetas api = actor.usando(UsarLaApiDeEtiquetas.class);
        Tags tags = api.getEtiquetasRecibidas();
        if (tags == null || tags.getTags() == null) {
            return Collections.emptyList();
        }
        return tags.getTags().stream()
                .map(org.xwiki.rest.model.jaxb.Tag::getName)
                .collect(Collectors.toList());
    }
}
