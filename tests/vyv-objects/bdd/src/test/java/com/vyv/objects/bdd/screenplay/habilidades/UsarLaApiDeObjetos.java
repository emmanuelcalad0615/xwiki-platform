package com.vyv.objects.bdd.screenplay.habilidades;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.MockedStatic;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.objects.ObjectResourceImpl;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Habilidad Screenplay (Ability): usar la API REST de objetos contra la clase
 * REAL {@link ObjectResourceImpl} de XWiki 18.4.0, con el wiki simulado
 * mediante dobles de prueba (la pagina y sus objetos viven en un almacen fake
 * en memoria).
 *
 * Debe cerrarse al final de cada escenario (libera el mock estatico de Utils).
 */
public final class UsarLaApiDeObjetos implements AutoCloseable
{
    public static final String WIKI = "xwiki";

    public static final String ESPACIO = "Main";

    public static final String PAGINA = "PruebaObjetos";

    private static final URI BASE_URI = URI.create("http://localhost:8080/rest/");

    private static final DocumentReference REFERENCIA = new DocumentReference(WIKI, ESPACIO, PAGINA);

    /** Identidad (clase, numero) de un objeto dentro de la pagina. */
    private record ClaveObjeto(String clase, int numero)
    {
    }

    private final ObjectResourceImpl recurso;

    private final ModelFactory modelFactory;

    private final ContextualAuthorizationManager autorizacion;

    private final Document documento;

    private final XWikiDocument documentoInterno;

    private final com.xpn.xwiki.api.XWiki apiXWiki;

    private final MockedStatic<Utils> utilsEstatico;

    /* Estado fake de la pagina (almacen en memoria) */
    private final Map<ClaveObjeto, com.xpn.xwiki.api.Object> objetos = new HashMap<>();

    private final Map<ClaveObjeto, BaseObject> objetosInternos = new HashMap<>();

    private boolean fallaAlmacenamiento;

    private int vecesGuardada;

    private int conversiones;

    /* Resultado de la ultima operacion del actor */
    private org.xwiki.rest.model.jaxb.Object objetoRecibido;

    private Response respuesta;

    private Integer codigoHttpDeError;

    private boolean errorInternoDelWiki;

    public UsarLaApiDeObjetos() throws Exception
    {
        this.recurso = new ObjectResourceImpl();
        this.modelFactory = mock(ModelFactory.class);
        this.autorizacion = mock(ContextualAuthorizationManager.class);
        this.documento = mock(Document.class);
        this.documentoInterno = mock(XWikiDocument.class);
        this.apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);

        configurarWikiSimulado();
        inyectarDependenciasReales();

        this.utilsEstatico = mockStatic(Utils.class);
        this.utilsEstatico.when(() -> Utils.getXWikiApi(any())).thenReturn(this.apiXWiki);
    }

    private void configurarWikiSimulado() throws Exception
    {
        // El documento existe y es accesible; sus objetos viven en el almacen fake.
        when(this.apiXWiki.getDocument(any(DocumentReference.class))).thenAnswer(invocation -> {
            if (this.fallaAlmacenamiento) {
                throw new XWikiException();
            }
            return this.documento;
        });
        when(this.documento.isNew()).thenReturn(false);
        when(this.documento.getDocumentReference()).thenReturn(REFERENCIA);
        when(this.documento.getObject(anyString(), anyInt())).thenAnswer(invocation ->
            this.objetos.get(new ClaveObjeto(invocation.getArgument(0), invocation.getArgument(1))));
        when(this.documento.removeObject(any())).thenAnswer(invocation -> {
            com.xpn.xwiki.api.Object objeto = invocation.getArgument(0);
            ClaveObjeto clave = claveDe(objeto);
            if (clave == null) {
                return false;
            }
            this.objetos.remove(clave);
            this.objetosInternos.remove(clave);
            return true;
        });
        doAnswer(invocation -> {
            this.vecesGuardada++;
            return null;
        }).when(this.documento).save();
        doAnswer(invocation -> {
            this.vecesGuardada++;
            return null;
        }).when(this.documento).save(anyString(), anyBoolean());

        // Vista interna (XWikiDocument) de la que getBaseObject lee los BaseObject.
        when(this.documentoInterno.getObject(anyString(), anyInt())).thenAnswer(invocation ->
            this.objetosInternos.get(new ClaveObjeto(invocation.getArgument(0), invocation.getArgument(1))));

        // La conversion a representacion REST refleja la identidad del objeto pedido.
        when(this.modelFactory.toRestObject(any(), any(), any(), anyBoolean(), any()))
            .thenAnswer(invocation -> {
                BaseObject interno = invocation.getArgument(2);
                org.xwiki.rest.model.jaxb.Object rest = new org.xwiki.rest.model.jaxb.Object();
                this.objetosInternos.entrySet().stream()
                    .filter(entrada -> entrada.getValue() == interno)
                    .findFirst()
                    .ifPresent(entrada -> {
                        rest.setClassName(entrada.getKey().clase());
                        rest.setNumber(entrada.getKey().numero());
                    });
                return rest;
            });
        doAnswer(invocation -> {
            this.conversiones++;
            return null;
        }).when(this.modelFactory).toObject(any(), any());

        // Por defecto el editor tiene permiso de edicion (se puede negar por paso Gherkin).
        when(this.autorizacion.hasAccess(any(Right.class), any())).thenReturn(true);
    }

    private void inyectarDependenciasReales() throws Exception
    {
        FieldUtils.writeField(this.recurso, "factory", this.modelFactory, true);
        FieldUtils.writeField(this.recurso, "authorization", this.autorizacion, true);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(BASE_URI);
        FieldUtils.writeField(this.recurso, "uriInfo", uriInfo, true);

        XWikiContext contexto = mock(XWikiContext.class);
        com.xpn.xwiki.XWiki nucleo = mock(com.xpn.xwiki.XWiki.class);
        when(contexto.getWiki()).thenReturn(nucleo);
        when(nucleo.getDocument(any(DocumentReference.class), any(XWikiContext.class)))
            .thenReturn(this.documentoInterno);
        Provider<XWikiContext> proveedor = () -> contexto;
        FieldUtils.writeField(this.recurso, "xcontextProvider", proveedor, true);
    }

    private ClaveObjeto claveDe(com.xpn.xwiki.api.Object objeto)
    {
        return this.objetos.entrySet().stream()
            .filter(entrada -> entrada.getValue() == objeto)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    /* ------------------- preparacion del escenario ------------------- */

    public void agregarObjeto(String clase, int numero)
    {
        ClaveObjeto clave = new ClaveObjeto(clase, numero);
        this.objetos.put(clave, mock(com.xpn.xwiki.api.Object.class));
        this.objetosInternos.put(clave, mock(BaseObject.class));
    }

    public void concederPermisoDeEdicion()
    {
        when(this.autorizacion.hasAccess(any(Right.class), any())).thenReturn(true);
    }

    public void negarPermisoDeEdicion()
    {
        when(this.autorizacion.hasAccess(any(Right.class), any())).thenReturn(false);
    }

    public void simularFallaDeAlmacenamiento()
    {
        this.fallaAlmacenamiento = true;
    }

    /* ------------------------ acciones del actor ---------------------- */

    public void consultarObjeto(String clase, int numero)
    {
        try {
            this.objetoRecibido = this.recurso.getObject(WIKI, ESPACIO, PAGINA, clase, numero, false);
        } catch (WebApplicationException e) {
            this.codigoHttpDeError = e.getResponse().getStatus();
        } catch (XWikiRestException e) {
            this.errorInternoDelWiki = true;
        }
    }

    public void actualizarObjeto(String clase, int numero)
    {
        try {
            this.respuesta = this.recurso.updateObject(WIKI, ESPACIO, PAGINA, clase, numero, false,
                new org.xwiki.rest.model.jaxb.Object());
        } catch (WebApplicationException e) {
            this.codigoHttpDeError = e.getResponse().getStatus();
        } catch (XWikiRestException e) {
            this.errorInternoDelWiki = true;
        }
    }

    public void eliminarObjeto(String clase, int numero)
    {
        try {
            this.recurso.deleteObject(WIKI, ESPACIO, PAGINA, clase, numero);
        } catch (WebApplicationException e) {
            this.codigoHttpDeError = e.getResponse().getStatus();
        } catch (XWikiRestException e) {
            this.errorInternoDelWiki = true;
        }
    }

    /* ----------------------- estado observable ------------------------ */

    public org.xwiki.rest.model.jaxb.Object getObjetoRecibido()
    {
        return this.objetoRecibido;
    }

    public Integer getCodigoHttpDeError()
    {
        return this.codigoHttpDeError;
    }

    public Integer getCodigoHttpDeRespuesta()
    {
        return this.respuesta == null ? null : this.respuesta.getStatus();
    }

    public boolean huboErrorInternoDelWiki()
    {
        return this.errorInternoDelWiki;
    }

    public int getVecesGuardada()
    {
        return this.vecesGuardada;
    }

    public int getConversiones()
    {
        return this.conversiones;
    }

    public boolean tieneObjeto(String clase, int numero)
    {
        return this.objetos.containsKey(new ClaveObjeto(clase, numero));
    }

    @Override
    public void close()
    {
        this.utilsEstatico.close();
    }
}
