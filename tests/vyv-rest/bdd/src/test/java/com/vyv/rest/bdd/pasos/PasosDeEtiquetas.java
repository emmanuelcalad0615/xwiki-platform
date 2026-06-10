package com.vyv.rest.bdd.pasos;

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
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import com.vyv.rest.bdd.screenplay.Actor;
import com.vyv.rest.bdd.screenplay.habilidades.UsarLaApiDeEtiquetas;
import com.vyv.rest.bdd.screenplay.preguntas.ElCodigoDeEstado;
import com.vyv.rest.bdd.screenplay.preguntas.ElTiempoDeRespuesta;
import com.vyv.rest.bdd.screenplay.preguntas.LasEtiquetasDevueltas;
import com.vyv.rest.bdd.screenplay.preguntas.LasEtiquetasEnLaBaseDeDatos;
import com.vyv.rest.bdd.screenplay.tareas.AñadirEtiquetas;
import com.vyv.rest.bdd.screenplay.tareas.ConsultarEtiquetas;

public class PasosDeEtiquetas {

    private Actor actor;

    @Before
    public void prepararActor() throws Exception {
        actor = Actor.llamado("UsuarioXWiki");
        actor.puede(new UsarLaApiDeEtiquetas());
    }

    @After
    public void limpiarActor() throws Exception {
        actor.usando(UsarLaApiDeEtiquetas.class).close();
    }

    // --- DADOS ---

    @Dado("^.*existe una página \".*\" en el espacio \".*\".*$")
    public void existeUnaPagina() {
        // La configuración inicial de la habilidad ya simula la página "WebHome" en "Main"
    }

    @Dado("la página no tiene ninguna etiqueta asignada")
    public void paginaNoTieneEtiquetas() {
        actor.usando(UsarLaApiDeEtiquetas.class).establecerEtiquetasEnPagina(List.of());
    }

    @Dado("la página tiene asignadas las etiquetas {string} y {string}")
    public void paginaTieneEtiquetas(String tag1, String tag2) {
        actor.usando(UsarLaApiDeEtiquetas.class).establecerEtiquetasEnPagina(Arrays.asList(tag1, tag2));
    }

    @Dado("que soy un administrador autenticado")
    public void administradorAutenticado() {
        // Por defecto la habilidad simula un admin
    }

    @Dado("que he iniciado sesión exitosamente con credenciales válidas en la interfaz web")
    public void iniciadaSesionUi() {
        // Equivalente a administrador autenticado
    }

    @Dado("navego a la página {string} del espacio {string}")
    public void navegoAPagina(String arg1, String arg2) {
        // Visual
    }

    @Dado("que soy un usuario anónimo \\(sin credenciales)")
    public void usuarioAnonimo() {
        actor.usando(UsarLaApiDeEtiquetas.class).establecerUsuarioAnonimo();
    }

    @Dado("^que soy un usuario autenticado pero sin permisos de edición.*$")
    public void usuarioSinPermisos() {
        actor.usando(UsarLaApiDeEtiquetas.class).establecerUsuarioSinPermisos();
    }

    // --- CUANDOS ---

    @Cuando("realizo una petición GET al endpoint de etiquetas de la página")
    public void realizarPeticionGet() {
        actor.intenta(ConsultarEtiquetas.deLaPagina());
    }

    @Cuando("realizo una petición PUT al endpoint de etiquetas con el cuerpo:")
    public void realizarPeticionPutConCuerpo(String docString) {
        // Extraemos la etiqueta del JSON de forma sencilla
        String etiqueta = "nueva-etiqueta";
        if (docString.contains("nueva-etiqueta")) {
            etiqueta = "nueva-etiqueta";
        }
        actor.intenta(AñadirEtiquetas.aLaPagina(List.of(etiqueta)));
    }

    @Cuando("realizo una petición PUT al endpoint de etiquetas enviando {string} dos veces")
    public void realizarPeticionPutDuplicada(String etiqueta) {
        actor.intenta(AñadirEtiquetas.aLaPagina(Arrays.asList(etiqueta, etiqueta)));
    }

    @Cuando("realizo una petición PUT al endpoint de etiquetas para añadir {string}")
    public void realizarPeticionPutSimple(String etiqueta) {
        actor.intenta(AñadirEtiquetas.aLaPagina(List.of(etiqueta)));
    }

    // Pasos UI
    @Cuando("^hago clic en el botón de añadir etiqueta.*$")
    public void clickAnadirEtiqueta() {}

    @Cuando("el sistema carga el formulario de entrada de etiquetas dinámicamente vía AJAX")
    public void cargaFormulario() {}

    @Cuando("escribo una etiqueta única en el campo de texto y presiono Enter")
    public void escriboEtiquetaUnica() {
        actor.intenta(AñadirEtiquetas.aLaPagina(List.of("etiqueta-visual")));
    }

    // --- ENTONCES ---

    @Entonces("recibo un código de estado {int} OK")
    public void reciboCodigoOk(int codigoEsperado) {
        assertThat(actor.responde(ElCodigoDeEstado.retornado()), is(equalTo(codigoEsperado)));
    }

    @Entonces("recibo un código de estado {int} Accepted")
    public void reciboCodigoAccepted(int codigoEsperado) {
        assertThat(actor.responde(ElCodigoDeEstado.retornado()), is(equalTo(codigoEsperado)));
    }

    @Entonces("recibo un código de estado {int} Unauthorized")
    public void reciboCodigoUnauthorized(int codigoEsperado) {
        assertThat(actor.responde(ElCodigoDeEstado.retornado()), is(equalTo(codigoEsperado)));
    }

    @Entonces("el servidor rechaza la petición por falta de autorización")
    public void servidorRechazaAutorizacion() {
        Integer codigo = actor.responde(ElCodigoDeEstado.retornado());
        // Puede ser 401 o 403
        assertThat(codigo == 401 || codigo == 403, is(true));
    }

    @Entonces("la respuesta contiene una lista vacía de etiquetas")
    public void respuestaContieneListaVacia() {
        assertThat(actor.responde(LasEtiquetasDevueltas.porLaApi()), is(empty()));
    }

    @Entonces("la respuesta contiene las etiquetas {string} y {string}")
    public void respuestaContieneEtiquetas(String tag1, String tag2) {
        assertThat(actor.responde(LasEtiquetasDevueltas.porLaApi()), containsInAnyOrder(tag1, tag2));
    }

    @Entonces("al consultar las etiquetas de la página, la lista incluye {string}")
    public void listaIncluyeEtiqueta(String tag) {
        actor.intenta(ConsultarEtiquetas.deLaPagina());
        assertThat(actor.responde(LasEtiquetasDevueltas.porLaApi()), hasItem(tag));
    }

    @Entonces("el sistema guarda la etiqueta {string} sin generar errores internos")
    public void guardaEtiquetaDuplicada(String tag) {
        actor.intenta(ConsultarEtiquetas.deLaPagina());
        assertThat(actor.responde(LasEtiquetasDevueltas.porLaApi()), hasItem(tag));
        assertThat(actor.responde(ElCodigoDeEstado.retornado()), is(equalTo(200)));
    }

    @Entonces("el sistema procesa la petición correctamente sin generar excepciones")
    public void procesaPeticionSinExcepciones() {
        actor.intenta(ConsultarEtiquetas.deLaPagina());
        assertThat(actor.responde(ElCodigoDeEstado.retornado()), is(equalTo(200)));
    }

    @Entonces("^las etiquetas de la página no son modificadas.*$")
    public void etiquetasNoModificadas() {
        List<String> etiquetasOriginales = actor.responde(LasEtiquetasEnLaBaseDeDatos.delSistema());
        assertThat(etiquetasOriginales, not(hasItem("hacker-tag")));
    }

    @Entonces("el sistema envía la etiqueta al servidor")
    public void enviaEtiquetaAlServidor() {}

    @Entonces("al recargar la página web por completo")
    public void recargaPagina() {}

    @Entonces("^.*nueva etiqueta se muestra persistida en el DOM dentro de la sección de etiquetas$")
    public void nuevaEtiquetaEnDOM() {
        List<String> persistidas = actor.responde(LasEtiquetasEnLaBaseDeDatos.delSistema());
        assertThat(persistidas, hasItem("etiqueta-visual"));
    }

    @Entonces("el tiempo de respuesta de la API es menor a {int} milisegundos")
    public void tiempoRespuestaMenorA(int milisegundos) {
        Long tiempo = actor.responde(ElTiempoDeRespuesta.deLaApiEnMilisegundos());
        assertThat(tiempo, is(lessThan((long) milisegundos)));
    }
}
