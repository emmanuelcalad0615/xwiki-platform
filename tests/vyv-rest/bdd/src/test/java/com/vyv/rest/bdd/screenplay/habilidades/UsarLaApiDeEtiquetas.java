package com.vyv.rest.bdd.screenplay.habilidades;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.MockedStatic;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.pages.PageTagsResourceImpl;
import org.xwiki.rest.model.jaxb.Tags;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Habilidad Screenplay: interactuar con la API REST de etiquetas simulando el wiki.
 */
public final class UsarLaApiDeEtiquetas implements AutoCloseable {
    public static final String WIKI = "xwiki";
    public static final String ESPACIO = "Main";
    public static final String PAGINA = "WebHome";
    private static final String PAGE_ID = "xwiki:Main.WebHome";
    private static final String TAG_CLASS = "XWiki.TagClass";

    private final PageTagsResourceImpl recurso;
    private final MockedStatic<Utils> utilsEstatico;

    private List<String> etiquetasDeLaPagina = new ArrayList<>();
    private boolean usuarioTienePermisoEdicion = true;
    private boolean usuarioEsAnonimo = false;

    // Estado observable tras llamar al recurso
    private Tags etiquetasRecibidas;
    private Integer codigoHttpRespuesta;
    private long tiempoRespuestaMs;

    public UsarLaApiDeEtiquetas() throws Exception {
        this.recurso = new PageTagsResourceImpl();
        
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/rest/"));
        FieldUtils.writeField(this.recurso, "uriInfo", uriInfo, true);

        org.xwiki.rest.model.jaxb.ObjectFactory factory = new org.xwiki.rest.model.jaxb.ObjectFactory();
        FieldUtils.writeField(this.recurso, "objectFactory", factory, true);

        this.utilsEstatico = mockStatic(Utils.class);
        configurarMocks();
    }

    private void configurarMocks() throws Exception {
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        javax.inject.Provider<XWikiContext> xcontextProvider = mock(javax.inject.Provider.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        FieldUtils.writeField(this.recurso, "xcontextProvider", xcontextProvider, true);

        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document apiDoc = mock(Document.class);
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenReturn(apiDoc);
        
        when(apiDoc.hasAccessLevel(eq("edit"), anyString())).thenAnswer(inv -> usuarioTienePermisoEdicion);
        when(apiDoc.getDocumentReference()).thenReturn(new DocumentReference(WIKI, ESPACIO, PAGINA));

        XWikiDocument xwikiDoc = mock(XWikiDocument.class);
        when(xwikiDoc.clone()).thenReturn(xwikiDoc);
        when(xwiki.getDocument(any(DocumentReference.class), any(XWikiContext.class))).thenReturn(xwikiDoc);
        when(xwiki.getDocument(eq(PAGE_ID), any(XWikiContext.class))).thenReturn(xwikiDoc);

        BaseObject tagObject = mock(BaseObject.class);
        BaseProperty tagProperty = mock(BaseProperty.class);
        
        when(xwikiDoc.getObject(TAG_CLASS)).thenReturn(tagObject);
        when(xwikiDoc.getObject(TAG_CLASS, 0)).thenReturn(tagObject);
        when(tagObject.safeget("tags")).thenReturn(tagProperty);
        when(tagProperty.getValue()).thenAnswer(inv -> etiquetasDeLaPagina);
        
        when(xwikiDoc.createNewObject(TAG_CLASS, xcontext)).thenReturn(0);
        when(tagObject.getClassName()).thenReturn(TAG_CLASS);
        
        BaseClass baseClass = mock(BaseClass.class);
        when(xwiki.getClass(TAG_CLASS, xcontext)).thenReturn(baseClass);
        when(baseClass.getPropertyNames()).thenReturn(new String[]{});

        this.utilsEstatico.when(() -> Utils.getPageId(anyString(), anyList(), anyString())).thenReturn(PAGE_ID);
        this.utilsEstatico.when(() -> Utils.getXWiki(any())).thenReturn(xwiki);
        this.utilsEstatico.when(() -> Utils.getXWikiContext(any())).thenReturn(xcontext);
        this.utilsEstatico.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
        this.utilsEstatico.when(() -> Utils.getXWikiUser(any())).thenAnswer(inv -> usuarioEsAnonimo ? "XWiki.XWikiGuest" : "XWiki.Admin");
        this.utilsEstatico.when(() -> Utils.createURI(any(), any(), any(Object[].class))).thenReturn(URI.create("http://localhost/dummy"));
    }

    // --- Preparación de Escenario ---
    public void establecerEtiquetasEnPagina(List<String> tags) {
        this.etiquetasDeLaPagina = new ArrayList<>(tags);
    }

    public void establecerUsuarioAnonimo() {
        this.usuarioEsAnonimo = true;
        this.usuarioTienePermisoEdicion = false;
    }

    public void establecerUsuarioSinPermisos() {
        this.usuarioEsAnonimo = false;
        this.usuarioTienePermisoEdicion = false;
    }

    // --- Acciones ---
    public void consultarEtiquetas() {
        long inicio = System.currentTimeMillis();
        try {
            this.etiquetasRecibidas = this.recurso.getPageTags(WIKI, ESPACIO, PAGINA);
            this.codigoHttpRespuesta = 200;
        } catch (WebApplicationException e) {
            this.codigoHttpRespuesta = e.getResponse().getStatus();
        } catch (XWikiRestException e) {
            this.codigoHttpRespuesta = 500;
        } finally {
            this.tiempoRespuestaMs = System.currentTimeMillis() - inicio;
        }
    }

    public void anadirEtiquetas(List<String> nombresEtiquetas) {
        Tags jaxbTags = new Tags();
        for (String n : nombresEtiquetas) {
            org.xwiki.rest.model.jaxb.Tag t = new org.xwiki.rest.model.jaxb.Tag();
            t.setName(n);
            jaxbTags.getTags().add(t);
        }
        
        long inicio = System.currentTimeMillis();
        try {
            Response respuestaPut = this.recurso.setTags(WIKI, ESPACIO, PAGINA, false, jaxbTags);
            this.codigoHttpRespuesta = respuestaPut.getStatus();
            if (this.codigoHttpRespuesta == Response.Status.ACCEPTED.getStatusCode()) {
                for (String t : nombresEtiquetas) {
                    if (!this.etiquetasDeLaPagina.contains(t)) {
                        this.etiquetasDeLaPagina.add(t);
                    }
                }
            }
        } catch (WebApplicationException e) {
            this.codigoHttpRespuesta = e.getResponse().getStatus();
        } catch (XWikiRestException e) {
            this.codigoHttpRespuesta = 500;
        } finally {
            this.tiempoRespuestaMs = System.currentTimeMillis() - inicio;
        }
    }

    // --- Estado Observable ---
    public Tags getEtiquetasRecibidas() {
        return this.etiquetasRecibidas;
    }

    public Integer getCodigoHttpRespuesta() {
        return this.codigoHttpRespuesta;
    }

    public long getTiempoRespuestaMs() {
        return this.tiempoRespuestaMs;
    }

    public List<String> getEtiquetasGuardadasEnBaseDeDatosFake() {
        return this.etiquetasDeLaPagina;
    }

    @Override
    public void close() {
        this.utilsEstatico.close();
    }
}
