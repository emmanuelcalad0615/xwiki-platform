package com.vyv.rest.bdd.screenplay.preguntas;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.rest.model.jaxb.Comments;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.Pregunta;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeComentarios;

public class LosComentariosDevueltos implements Pregunta<List<String>> {

    public static LosComentariosDevueltos porLaApi() {
        return new LosComentariosDevueltos();
    }

    @Override
    public List<String> respondidaPor(Actor actor) {
        Comments comments = actor.usando(UsarLaApiDeComentarios.class).getComentariosRecibidos();
        if (comments == null || comments.getComments() == null) {
            return Collections.emptyList();
        }
        return comments.getComments().stream()
                .map(Comment::getText)
                .collect(Collectors.toList());
    }
}
