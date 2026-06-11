package com.vyv.rest.bdd.screenplay.habilidades;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.MockedStatic;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.comments.CommentResourceImpl;
import org.xwiki.rest.internal.resources.comments.CommentsResourceImpl;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.rest.model.jaxb.Comments;
import org.xwiki.rest.model.jaxb.ObjectFactory;

import com.xpn.xwiki.api.Document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Habilidad Screenplay: interactuar con la API REST de comentarios (clases reales
 * CommentsResourceImpl y CommentResourceImpl de XWiki 18.4.0) simulando el wiki
 * con Mockito (mismo enfoque que las pruebas unitarias).
 */
public final class UsarLaApiDeComentarios implements AutoCloseable {
    public static final String WIKI = "xwiki";
    public static final String ESPACIO = "Main";
    public static final String PAGINA = "WebHome";
    private static final String COMMENTS_CLASS = "XWiki.XWikiComments";

    private final CommentsResourceImpl recursoComentarios;
    private final CommentResourceImpl recursoComentario;
    private final MockedStatic<Utils> utilsEstatico;
    private final MockedStatic<DomainObjectFactory> dofEstatico;

    private final Document doc;
    private final com.xpn.xwiki.api.XWiki apiXWiki;

    // "Base de datos" fake: comentarios de la pagina
    private final Vector<com.xpn.xwiki.api.Object> comentariosEnPagina = new Vector<>();
    private final Map<com.xpn.xwiki.api.Object, String> textoPorObjeto = new HashMap<>();

    private boolean usuarioConPermiso = true;
    private boolean usuarioAnonimo = false;

    // Estado observable tras llamar al recurso
    private Comments comentariosRecibidos;
    private Comment comentarioRecibido;
    private Integer codigoHttpRespuesta;
    private long tiempoRespuestaMs;

    public UsarLaApiDeComentarios() throws Exception {
        this.recursoComentarios = new CommentsResourceImpl();
        this.recursoComentario = new CommentResourceImpl();

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/rest/"));
        ObjectFactory factory = new ObjectFactory();
        for (Object recurso : new Object[] {recursoComentarios, recursoComentario}) {
            FieldUtils.writeField(recurso, "uriInfo", uriInfo, true);
            FieldUtils.writeField(recurso, "objectFactory", factory, true);
        }

        this.doc = mock(Document.class);
        this.apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);

        this.utilsEstatico = mockStatic(Utils.class);
        this.dofEstatico = mockStatic(DomainObjectFactory.class);
        configurarMocks();
    }

    @SuppressWarnings("unchecked")
    private void configurarMocks() throws Exception {
        when(doc.isNew()).thenReturn(false);
        when(doc.getLocked()).thenReturn(false);
        when(doc.getComments()).thenReturn(comentariosEnPagina);

        // Sin permiso/anonimo => getDocument devuelve null => el recurso lanza 401
        when(apiXWiki.getDocument(any(DocumentReference.class)))
            .thenAnswer(inv -> usuarioConPermiso ? doc : null);

        // POST: crear un nuevo objeto comentario en la pagina
        when(doc.createNewObject(COMMENTS_CLASS)).thenAnswer(inv -> comentariosEnPagina.size());
        com.xpn.xwiki.api.Object nuevoComentario = mock(com.xpn.xwiki.api.Object.class);
        when(doc.getObject(eq(COMMENTS_CLASS), any(Integer.class))).thenReturn(nuevoComentario);

        utilsEstatico.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
        utilsEstatico.when(() -> Utils.getXWikiUser(any()))
            .thenAnswer(inv -> usuarioAnonimo ? "XWiki.XWikiGuest" : "XWiki.Admin");
        utilsEstatico.when(() -> Utils.createURI(any(), any(), any(), any(), any(), any()))
            .thenReturn(URI.create("http://localhost/dummy"));

        // createComment: construye un Comment con el texto guardado para ese objeto
        dofEstatico.when(() -> DomainObjectFactory.createComment(any(), any(), any(), any(), any(), any()))
            .thenAnswer(inv -> {
                com.xpn.xwiki.api.Object objeto = inv.getArgument(3);
                Comment c = new Comment();
                c.setText(textoPorObjeto.getOrDefault(objeto, ""));
                if (objeto != null) {
                    c.setId(objeto.getNumber());
                }
                return c;
            });
    }

    // --- Preparacion de escenario ---
    public void establecerComentariosEnPagina(List<String> textos) {
        comentariosEnPagina.clear();
        textoPorObjeto.clear();
        for (String t : textos) {
            agregarComentarioFake(t);
        }
    }

    private void agregarComentarioFake(String texto) {
        com.xpn.xwiki.api.Object objeto = mock(com.xpn.xwiki.api.Object.class);
        when(objeto.getNumber()).thenReturn(comentariosEnPagina.size());
        comentariosEnPagina.add(objeto);
        textoPorObjeto.put(objeto, texto);
    }

    public void establecerUsuarioAnonimo() {
        this.usuarioAnonimo = true;
        this.usuarioConPermiso = false;
    }

    public void establecerUsuarioSinPermisos() {
        this.usuarioAnonimo = false;
        this.usuarioConPermiso = false;
    }

    // --- Acciones ---
    public void consultarComentarios() {
        long inicio = System.currentTimeMillis();
        try {
            this.comentariosRecibidos = recursoComentarios.getComments(WIKI, ESPACIO, PAGINA, 0, 50, false);
            this.codigoHttpRespuesta = 200;
        } catch (WebApplicationException e) {
            this.codigoHttpRespuesta = e.getResponse().getStatus();
        } catch (XWikiRestException e) {
            this.codigoHttpRespuesta = 500;
        } finally {
            this.tiempoRespuestaMs = System.currentTimeMillis() - inicio;
        }
    }

    public void crearComentario(String texto) {
        Comment input = new Comment();
        if (texto != null && !texto.isEmpty()) {
            input.setText(texto);
        }
        long inicio = System.currentTimeMillis();
        try {
            Response r = recursoComentarios.postComment(WIKI, ESPACIO, PAGINA, input);
            this.codigoHttpRespuesta = (r == null) ? Response.Status.NO_CONTENT.getStatusCode() : r.getStatus();
            if (this.codigoHttpRespuesta == Response.Status.CREATED.getStatusCode()) {
                agregarComentarioFake(texto);
            }
        } catch (WebApplicationException e) {
            this.codigoHttpRespuesta = e.getResponse().getStatus();
        } catch (XWikiRestException e) {
            this.codigoHttpRespuesta = 500;
        } finally {
            this.tiempoRespuestaMs = System.currentTimeMillis() - inicio;
        }
    }

    public void obtenerComentario(int id) {
        long inicio = System.currentTimeMillis();
        try {
            this.comentarioRecibido = recursoComentario.getComment(WIKI, ESPACIO, PAGINA, id, 0, 50, false);
            this.codigoHttpRespuesta = 200;
        } catch (WebApplicationException e) {
            this.codigoHttpRespuesta = e.getResponse().getStatus();
        } catch (XWikiRestException e) {
            this.codigoHttpRespuesta = 500;
        } finally {
            this.tiempoRespuestaMs = System.currentTimeMillis() - inicio;
        }
    }

    // --- Estado observable ---
    public Comments getComentariosRecibidos() {
        return this.comentariosRecibidos;
    }

    public Comment getComentarioRecibido() {
        return this.comentarioRecibido;
    }

    public Integer getCodigoHttpRespuesta() {
        return this.codigoHttpRespuesta;
    }

    public long getTiempoRespuestaMs() {
        return this.tiempoRespuestaMs;
    }

    public List<String> getComentariosGuardadosEnBaseDeDatosFake() {
        return new ArrayList<>(textoPorObjeto.values());
    }

    @Override
    public void close() {
        this.utilsEstatico.close();
        this.dofEstatico.close();
    }
}
