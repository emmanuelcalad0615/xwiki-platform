package com.vyv.rest.bdd.comentarios;

import java.util.Arrays;
import java.util.List;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeComentarios;
import com.vyv.rest.bdd.screenplay.preguntas.ElCodigoDeEstadoDeComentarios;
import com.vyv.rest.bdd.screenplay.preguntas.LosComentariosDevueltos;
import com.vyv.rest.bdd.screenplay.tareas.ConsultarComentarios;
import com.vyv.rest.bdd.screenplay.tareas.CrearComentario;
import com.vyv.rest.bdd.screenplay.tareas.ObtenerComentario;

/**
 * Step definitions (glue) de los escenarios Gherkin de comentarios. En su propio
 * paquete para no colisionar con los pasos de etiquetas (mismas frases comunes).
 */
public class PasosDeComentarios {

    private Actor actor;

    @Before
    public void prepararActor() throws Exception {
        actor = Actor.llamado("UsuarioXWiki");
        actor.puede(new UsarLaApiDeComentarios());
    }

    @After
    public void limpiarActor() {
        actor.usando(UsarLaApiDeComentarios.class).close();
    }

    // --- DADOS ---

    @Dado("^.*existe una página \".*\" en el espacio \".*\".*$")
    public void existeUnaPagina() {
        // La habilidad ya simula la pagina "WebHome" del espacio "Main"
    }

    @Dado("la página no tiene ningún comentario")
    public void paginaSinComentarios() {
        actor.usando(UsarLaApiDeComentarios.class).establecerComentariosEnPagina(List.of());
    }

    @Dado("la página tiene los comentarios {string} y {string}")
    public void paginaTieneDosComentarios(String c1, String c2) {
        actor.usando(UsarLaApiDeComentarios.class).establecerComentariosEnPagina(Arrays.asList(c1, c2));
    }

    @Dado("la página tiene los comentarios {string}")
    public void paginaTieneUnComentario(String c1) {
        actor.usando(UsarLaApiDeComentarios.class).establecerComentariosEnPagina(List.of(c1));
    }

    @Dado("que soy un administrador autenticado")
    public void administradorAutenticado() {
        // Por defecto la habilidad simula un admin con permisos
    }

    @Dado("que soy un usuario anónimo \\(sin credenciales)")
    public void usuarioAnonimo() {
        actor.usando(UsarLaApiDeComentarios.class).establecerUsuarioAnonimo();
    }

    // --- CUANDOS ---

    @Cuando("realizo una petición GET al endpoint de comentarios de la página")
    public void getComentarios() {
        actor.intenta(ConsultarComentarios.deLaPagina());
    }

    @Cuando("realizo una petición POST para crear el comentario {string}")
    public void postComentario(String texto) {
        actor.intenta(CrearComentario.conTexto(texto));
    }

    @Cuando("realizo una petición POST para crear un comentario sin contenido")
    public void postComentarioVacio() {
        actor.intenta(CrearComentario.conTexto(""));
    }

    @Cuando("realizo una petición GET para obtener el comentario con id {int}")
    public void getComentarioPorId(int id) {
        actor.intenta(ObtenerComentario.conId(id));
    }

    // --- ENTONCES ---

    @Entonces("recibo un código de estado {int} OK")
    public void codigoOk(int esperado) {
        assertThat(actor.responde(ElCodigoDeEstadoDeComentarios.retornado()), is(equalTo(esperado)));
    }

    @Entonces("recibo un código de estado {int} Created")
    public void codigoCreated(int esperado) {
        assertThat(actor.responde(ElCodigoDeEstadoDeComentarios.retornado()), is(equalTo(esperado)));
    }

    @Entonces("recibo un código de estado {int} Not Found")
    public void codigoNotFound(int esperado) {
        assertThat(actor.responde(ElCodigoDeEstadoDeComentarios.retornado()), is(equalTo(esperado)));
    }

    @Entonces("el sistema responde con un código de estado {int} No Content")
    public void codigoNoContent(int esperado) {
        assertThat(actor.responde(ElCodigoDeEstadoDeComentarios.retornado()), is(equalTo(esperado)));
    }

    @Entonces("el servidor rechaza la petición por falta de autorización")
    public void rechazaAutorizacion() {
        Integer codigo = actor.responde(ElCodigoDeEstadoDeComentarios.retornado());
        assertThat(codigo == 401 || codigo == 403, is(true));
    }

    @Entonces("la respuesta contiene una lista vacía de comentarios")
    public void listaVacia() {
        assertThat(actor.responde(LosComentariosDevueltos.porLaApi()), is(empty()));
    }

    @Entonces("la respuesta contiene los comentarios {string} y {string}")
    public void respuestaContieneComentarios(String c1, String c2) {
        assertThat(actor.responde(LosComentariosDevueltos.porLaApi()), containsInAnyOrder(c1, c2));
    }

    @Entonces("al consultar los comentarios de la página, la lista incluye {string}")
    public void listaIncluye(String texto) {
        actor.intenta(ConsultarComentarios.deLaPagina());
        assertThat(actor.responde(LosComentariosDevueltos.porLaApi()), hasItem(texto));
    }
}
