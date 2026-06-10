package com.vyv.objects.bdd.pasos;

import com.vyv.objects.bdd.screenplay.Actor;
import com.vyv.objects.bdd.screenplay.habilidades.UsarLaApiDeObjetos;
import com.vyv.objects.bdd.screenplay.preguntas.EstadoDeLaPagina;
import com.vyv.objects.bdd.screenplay.preguntas.Respuestas;
import com.vyv.objects.bdd.screenplay.tareas.ActualizarObjeto;
import com.vyv.objects.bdd.screenplay.tareas.ConsultarObjeto;
import com.vyv.objects.bdd.screenplay.tareas.EliminarObjeto;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Step definitions (glue) de los escenarios Gherkin de la funcionalidad
 * "objects". Cada paso delega en el patron Screenplay: el actor "el editor"
 * ejecuta tareas y responde preguntas usando su habilidad
 * {@link UsarLaApiDeObjetos}.
 */
public class PasosDeObjetos
{
    private Actor editor;

    private UsarLaApiDeObjetos api;

    @Before
    public void prepararEscenario() throws Exception
    {
        this.api = new UsarLaApiDeObjetos();
        this.editor = Actor.llamado("el editor").puede(this.api);
    }

    @After
    public void limpiarEscenario()
    {
        if (this.api != null) {
            this.api.close();
        }
    }

    /* ------------------------------ Dado ------------------------------ */

    @Dado("que la página {string} del wiki {string} existe")
    public void laPaginaDelWikiExiste(String pagina, String wiki)
    {
        // La habilidad ya modela una pagina existente y accesible; se validan los nombres
        // del escenario para que el feature y el montaje no se desincronicen.
        assertThat(wiki, is(UsarLaApiDeObjetos.WIKI));
        assertThat(pagina, is(UsarLaApiDeObjetos.ESPACIO + "." + UsarLaApiDeObjetos.PAGINA));
    }

    @Dado("que la página tiene un objeto {string} con número {int}")
    public void laPaginaTieneUnObjeto(String clase, int numero)
    {
        this.api.agregarObjeto(clase, numero);
    }

    @Y("que el editor tiene permiso de edición sobre la página")
    public void elEditorTienePermisoDeEdicion()
    {
        this.api.concederPermisoDeEdicion();
    }

    @Y("que el editor no tiene permiso de edición sobre la página")
    public void elEditorNoTienePermisoDeEdicion()
    {
        this.api.negarPermisoDeEdicion();
    }

    @Dado("que el almacenamiento del wiki está fallando")
    public void elAlmacenamientoEstaFallando()
    {
        this.api.simularFallaDeAlmacenamiento();
    }

    /* ----------------------------- Cuando ----------------------------- */

    @Cuando("el editor consulta el objeto {string} número {int}")
    public void elEditorConsultaElObjeto(String clase, int numero)
    {
        this.editor.intenta(ConsultarObjeto.numerado(clase, numero));
    }

    @Cuando("el editor actualiza el objeto {string} número {int}")
    public void elEditorActualizaElObjeto(String clase, int numero)
    {
        this.editor.intenta(ActualizarObjeto.numerado(clase, numero));
    }

    @Cuando("el editor elimina el objeto {string} número {int}")
    public void elEditorEliminaElObjeto(String clase, int numero)
    {
        this.editor.intenta(EliminarObjeto.numerado(clase, numero));
    }

    /* ---------------------------- Entonces ---------------------------- */

    @Entonces("recibe el objeto con clase {string} y número {int}")
    public void recibeElObjeto(String clase, int numero)
    {
        org.xwiki.rest.model.jaxb.Object recibido = this.editor.responde(Respuestas.elObjetoRecibido());
        assertThat(recibido, is(notNullValue()));
        assertThat(recibido.getClassName(), is(clase));
        assertThat(recibido.getNumber(), is(numero));
    }

    @Entonces("la operación falla con el código HTTP {int}")
    public void laOperacionFallaConCodigo(int codigo)
    {
        assertThat(this.editor.responde(Respuestas.elCodigoHttpDeError()), is(codigo));
    }

    @Entonces("la operación responde con el código HTTP {int}")
    public void laOperacionRespondeConCodigo(int codigo)
    {
        assertThat(this.editor.responde(Respuestas.elCodigoHttpDeRespuesta()), is(codigo));
    }

    @Entonces("la operación falla con un error interno del wiki")
    public void laOperacionFallaConErrorInterno()
    {
        assertThat(this.editor.responde(Respuestas.siHuboErrorInternoDelWiki()), is(true));
    }

    @Y("la página se guarda")
    public void laPaginaSeGuarda()
    {
        assertThat(this.editor.responde(EstadoDeLaPagina.lasVecesQueSeGuardo()), is(greaterThanOrEqualTo(1)));
    }

    @Y("la página se guarda con los cambios")
    public void laPaginaSeGuardaConLosCambios()
    {
        assertThat(this.editor.responde(EstadoDeLaPagina.lasVecesQueSeGuardo()), is(greaterThanOrEqualTo(1)));
        assertThat(this.editor.responde(EstadoDeLaPagina.losCambiosAplicados()), is(greaterThanOrEqualTo(1)));
    }

    @Y("la página no se guarda")
    public void laPaginaNoSeGuarda()
    {
        assertThat(this.editor.responde(EstadoDeLaPagina.lasVecesQueSeGuardo()), is(0));
    }

    @Entonces("el objeto {string} número {int} ya no está en la página")
    public void elObjetoYaNoEstaEnLaPagina(String clase, int numero)
    {
        assertThat(this.editor.responde(EstadoDeLaPagina.siTieneElObjeto(clase, numero)), is(false));
    }

    @Y("el objeto {string} número {int} sigue en la página")
    public void elObjetoSigueEnLaPagina(String clase, int numero)
    {
        assertThat(this.editor.responde(EstadoDeLaPagina.siTieneElObjeto(clase, numero)), is(true));
    }
}
