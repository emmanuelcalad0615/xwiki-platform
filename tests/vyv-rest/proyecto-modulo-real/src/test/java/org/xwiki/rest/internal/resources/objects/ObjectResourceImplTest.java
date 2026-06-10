/*
 * Pruebas unitarias automatizadas de ObjectResourceImpl (ver, actualizar y borrar
 * un objeto de una pagina). Funcionalidad: "objects".
 *
 * Patron AAA + principios FIRST. Los 5 dobles de prueba:
 *  - DUMMY : parametro withPrettyNames (se pasa pero no influye en el resultado evaluado)
 *            y restObject en los caminos 401 (nunca se usa).
 *  - FAKE  : FakeXWikiContextProvider (Provider real en memoria) y
 *            FakeAlmacenDeObjetos (almacen en memoria que respalda getObject del documento).
 *  - STUB  : todos los when(...).thenReturn(...) de XWiki api, Document y ModelFactory.
 *  - SPY   : spy sobre el FakeXWikiContextProvider; se verifica que el codigo real
 *            consulta el contexto (getBaseObject) con verify(spy, atLeastOnce()).get().
 *  - MOCK  : Document, XWiki (api y core), XWikiDocument, api.Object, ModelFactory y
 *            ContextualAuthorizationManager, verificados con verify(...).
 *
 * Caminos cubiertos (14 tests = 100% lineas y ramas de ObjectResourceImpl):
 *  getObject    : exito | objeto inexistente 404 | pagina inexistente 404 | XWikiException -> XWikiRestException
 *                 | wiki nulo -> IllegalArgumentException (caso frontera propuesto por la IA del equipo)
 *  updateObject : sin permiso EDIT 401 | objeto inexistente 404 | exito 202 + save("",false)
 *                 | revision menor save("",true) | fallo al guardar -> XWikiRestException
 *  deleteObject : sin permiso EDIT 401 | objeto inexistente 404 | exito removeObject+save
 *                 | fallo al guardar -> XWikiRestException
 *
 * Ejecutar (proyecto independiente, sin compilar el monorepo):
 *   mvn test -f tests/vyv-rest/proyecto-modulo-real/pom.xml -Dtest="ObjectResourceImplTest"
 * O con el flujo del equipo (worktree xwiki-184):
 *   .\correr.ps1 equipo
 */
package org.xwiki.rest.internal.resources.objects;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ObjectResourceImpl} contra la clase REAL de XWiki.
 */
@ComponentTest
class ObjectResourceImplTest
{
    private static final String WIKI = "xwiki";

    private static final String SPACE = "Main";

    private static final String PAGE = "PruebaObjetos";

    private static final String CLASS_NAME = "XWiki.TagClass";

    private static final int NUMERO = 0;

    private static final URI BASE_URI = URI.create("http://localhost:8080/rest/");

    private static final DocumentReference DOC_REFERENCE = new DocumentReference(WIKI, SPACE, PAGE);

    /** MOCK (componente): convierte entre objetos del modelo y su representacion REST. */
    @MockComponent
    private ModelFactory modelFactory;

    /** MOCK (componente): decide si el usuario actual tiene un permiso. */
    @MockComponent
    private ContextualAuthorizationManager authorization;

    /** Clase REAL bajo prueba, inyectada por el framework de componentes de XWiki. */
    @InjectMockComponents
    private ObjectResourceImpl objectResource;

    /** MOCK: doble de UriInfo (en produccion lo aporta JAX-RS). */
    private UriInfo uriInfo;

    /** SPY sobre un FAKE: provider del contexto XWiki; permite verificar que se consulta. */
    private Provider<XWikiContext> providerSpy;

    /** MOCK: documento interno (XWikiDocument) del que se leen los BaseObject. */
    private XWikiDocument xwikiDocument;

    @BeforeEach
    void setUp() throws Exception
    {
        this.uriInfo = mock(UriInfo.class);                                   // MOCK
        when(this.uriInfo.getBaseUri()).thenReturn(BASE_URI);                 // STUB
        FieldUtils.writeField(this.objectResource, "uriInfo", this.uriInfo, true);
    }

    /* ------------------------------------------------------------------ */
    /* Dobles auxiliares                                                    */
    /* ------------------------------------------------------------------ */

    /** FAKE: implementacion real y ligera de Provider, sin contenedor de inyeccion. */
    static class FakeXWikiContextProvider implements Provider<XWikiContext>
    {
        private final XWikiContext context;

        FakeXWikiContextProvider(XWikiContext context)
        {
            this.context = context;
        }

        @Override
        public XWikiContext get()
        {
            return this.context;
        }
    }

    /** FAKE: almacen en memoria de objetos por numero (respalda XWikiDocument.getObject). */
    static class FakeAlmacenDeObjetos
    {
        private final Map<Integer, BaseObject> objetos = new HashMap<>();

        void guardar(int numero, BaseObject objeto)
        {
            this.objetos.put(numero, objeto);
        }

        BaseObject buscar(int numero)
        {
            return this.objetos.get(numero);
        }
    }

    /** Crea el documento api accesible (existe y se puede leer). */
    private Document mockAccessibleDocument(com.xpn.xwiki.api.XWiki apiXWiki) throws Exception
    {
        Document doc = mock(Document.class);                                      // MOCK
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenReturn(doc); // STUB
        when(doc.isNew()).thenReturn(false);                                      // STUB
        when(doc.getDocumentReference()).thenReturn(DOC_REFERENCE);               // STUB
        return doc;
    }

    /**
     * Inyecta el contexto XWiki (FAKE espiado) y el documento interno del que
     * getBaseObject lee los objetos. Devuelve el XWikiDocument mockeado.
     */
    private XWikiDocument injectXWikiContext() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);                     // MOCK
        com.xpn.xwiki.XWiki coreXWiki = mock(com.xpn.xwiki.XWiki.class);      // MOCK
        this.xwikiDocument = mock(XWikiDocument.class);                       // MOCK
        when(xcontext.getWiki()).thenReturn(coreXWiki);                       // STUB
        when(coreXWiki.getDocument(any(DocumentReference.class), any(XWikiContext.class)))
            .thenReturn(this.xwikiDocument);                                  // STUB

        this.providerSpy = spy(new FakeXWikiContextProvider(xcontext));       // SPY sobre FAKE
        FieldUtils.writeField(this.objectResource, "xcontextProvider", this.providerSpy, true);
        return this.xwikiDocument;
    }

    /* ------------------------------------------------------------------ */
    /* getObject                                                            */
    /* ------------------------------------------------------------------ */

    @Test
    void getObject_WhenObjectExists_ShouldReturnRestObject() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class); // MOCK
        Document doc = mockAccessibleDocument(apiXWiki);
        XWikiDocument xdoc = injectXWikiContext();

        BaseObject baseObject = mock(BaseObject.class);                       // MOCK
        FakeAlmacenDeObjetos almacen = new FakeAlmacenDeObjetos();            // FAKE
        almacen.guardar(NUMERO, baseObject);
        when(xdoc.getObject(eq(CLASS_NAME), anyInt()))
            .thenAnswer(invocation -> almacen.buscar(invocation.getArgument(1))); // STUB -> FAKE

        org.xwiki.rest.model.jaxb.Object esperado = new org.xwiki.rest.model.jaxb.Object();
        esperado.setClassName(CLASS_NAME);
        esperado.setNumber(NUMERO);
        when(this.modelFactory.toRestObject(eq(BASE_URI), same(doc), same(baseObject), eq(false), eq(false)))
            .thenReturn(esperado);                                            // STUB

        Boolean dummyPrettyNames = Boolean.FALSE;                             // DUMMY

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act
            org.xwiki.rest.model.jaxb.Object resultado =
                this.objectResource.getObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO, dummyPrettyNames);

            // Assert (aserciones fluidas)
            assertThat(resultado, is(notNullValue()));
            assertThat(resultado, is(sameInstance(esperado)));
            assertThat(resultado.getClassName(), is(CLASS_NAME));
            assertThat(resultado.getNumber(), is(NUMERO));
            verify(this.modelFactory).toRestObject(eq(BASE_URI), same(doc), same(baseObject), eq(false), eq(false));
            verify(this.providerSpy, atLeastOnce()).get();                    // SPY: se consulto el contexto
        }
    }

    @Test
    void getObject_WhenObjectDoesNotExist_ShouldThrowNotFound() throws Exception
    {
        // Arrange: el almacen FAKE no tiene el numero pedido -> getObject devuelve null
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        mockAccessibleDocument(apiXWiki);
        XWikiDocument xdoc = injectXWikiContext();

        FakeAlmacenDeObjetos almacen = new FakeAlmacenDeObjetos();            // FAKE vacio
        when(xdoc.getObject(eq(CLASS_NAME), anyInt()))
            .thenAnswer(invocation -> almacen.buscar(invocation.getArgument(1))); // STUB -> FAKE

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.objectResource.getObject(WIKI, SPACE, PAGE, CLASS_NAME, 99, false));
            assertThat(ex.getResponse().getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    void getObject_WhenPageDoesNotExist_ShouldThrowNotFound() throws Exception
    {
        // Arrange: la pagina es nueva (no existe) -> getDocumentInfo corta con 404
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mock(Document.class);                                  // MOCK
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenReturn(doc); // STUB
        when(doc.isNew()).thenReturn(true);                                   // STUB: pagina inexistente

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.objectResource.getObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO, false));
            assertThat(ex.getResponse().getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    void getObject_WhenWikiNameIsNull_ShouldThrowIllegalArgumentException()
    {
        // Caso frontera propuesto por la herramienta IA del equipo (generador de
        // casos): los nombres nulos no llegan al almacenamiento, getDocumentInfo
        // los rechaza de entrada con IllegalArgumentException.
        // Arrange: sin wiki valido no hace falta preparar ningun doble mas.

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
            () -> this.objectResource.getObject(null, SPACE, PAGE, CLASS_NAME, NUMERO, false));
    }

    @Test
    void getObject_WhenXWikiFails_ShouldThrowXWikiRestException() throws Exception
    {
        // Arrange (fault injection): el acceso al documento revienta con XWikiException
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenThrow(new XWikiException()); // STUB

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert: la clase real envuelve la falla en XWikiRestException
            XWikiRestException ex = assertThrows(XWikiRestException.class,
                () -> this.objectResource.getObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO, false));
            assertThat(ex.getCause(), is(instanceOf(XWikiException.class)));
        }
    }

    /* ------------------------------------------------------------------ */
    /* updateObject                                                         */
    /* ------------------------------------------------------------------ */

    @Test
    void updateObject_WhenUserHasNoEditRight_ShouldThrowUnauthorized() throws Exception
    {
        // Arrange: la autorizacion niega EDIT sobre el documento
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        when(this.authorization.hasAccess(Right.EDIT, DOC_REFERENCE)).thenReturn(false); // STUB

        org.xwiki.rest.model.jaxb.Object dummyRestObject = new org.xwiki.rest.model.jaxb.Object(); // DUMMY

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.objectResource.updateObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO, false,
                    dummyRestObject));
            assertThat(ex.getResponse().getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
            verify(this.authorization).hasAccess(Right.EDIT, DOC_REFERENCE);  // MOCK verificado
            verify(doc, never()).save(anyString(), anyBoolean());             // no se guardo nada
            verify(this.modelFactory, never()).toObject(any(), any());        // ni se convirtio nada
        }
    }

    @Test
    void updateObject_WhenObjectDoesNotExist_ShouldThrowNotFound() throws Exception
    {
        // Arrange: hay permiso, pero la pagina no tiene el objeto pedido
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        when(this.authorization.hasAccess(Right.EDIT, DOC_REFERENCE)).thenReturn(true); // STUB
        when(doc.getObject(CLASS_NAME, NUMERO)).thenReturn(null);             // STUB: no existe

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.objectResource.updateObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO, false,
                    new org.xwiki.rest.model.jaxb.Object()));
            assertThat(ex.getResponse().getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
            verify(doc, never()).save(anyString(), anyBoolean());
        }
    }

    @Test
    void updateObject_WhenValid_ShouldSaveAndReturnAccepted() throws Exception
    {
        // Arrange: permiso concedido, el objeto existe y la conversion es valida
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        XWikiDocument xdoc = injectXWikiContext();
        when(this.authorization.hasAccess(Right.EDIT, DOC_REFERENCE)).thenReturn(true); // STUB

        com.xpn.xwiki.api.Object xwikiObject = mock(com.xpn.xwiki.api.Object.class);    // MOCK
        when(doc.getObject(CLASS_NAME, NUMERO)).thenReturn(xwikiObject);      // STUB

        BaseObject baseObject = mock(BaseObject.class);                       // MOCK
        when(xdoc.getObject(CLASS_NAME, NUMERO)).thenReturn(baseObject);      // STUB

        org.xwiki.rest.model.jaxb.Object enviado = new org.xwiki.rest.model.jaxb.Object();
        org.xwiki.rest.model.jaxb.Object actualizado = new org.xwiki.rest.model.jaxb.Object();
        actualizado.setClassName(CLASS_NAME);
        actualizado.setNumber(NUMERO);
        when(this.modelFactory.toRestObject(eq(BASE_URI), same(doc), same(baseObject), eq(false), eq(false)))
            .thenReturn(actualizado);                                         // STUB

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act (minorRevision null -> guardado normal, rama Boolean.TRUE.equals == false)
            Response respuesta = this.objectResource.updateObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO,
                null, enviado);

            // Assert
            assertThat(respuesta.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
            assertThat(respuesta.getEntity(), is(sameInstance(actualizado)));
            InOrder orden = inOrder(this.modelFactory, doc);                  // MOCKs verificados en orden
            orden.verify(this.modelFactory).toObject(same(xwikiObject), same(enviado));
            orden.verify(doc).save("", false);
            verify(this.providerSpy, atLeastOnce()).get();                    // SPY: se consulto el contexto
        }
    }

    @Test
    void updateObject_WhenMinorRevision_ShouldSaveAsMinorEdit() throws Exception
    {
        // Arrange: igual al caso valido pero pidiendo revision menor
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        XWikiDocument xdoc = injectXWikiContext();
        when(this.authorization.hasAccess(Right.EDIT, DOC_REFERENCE)).thenReturn(true);

        com.xpn.xwiki.api.Object xwikiObject = mock(com.xpn.xwiki.api.Object.class);
        when(doc.getObject(CLASS_NAME, NUMERO)).thenReturn(xwikiObject);
        when(xdoc.getObject(CLASS_NAME, NUMERO)).thenReturn(mock(BaseObject.class));
        when(this.modelFactory.toRestObject(any(), any(), any(), anyBoolean(), any()))
            .thenReturn(new org.xwiki.rest.model.jaxb.Object());              // STUB

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act
            Response respuesta = this.objectResource.updateObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO,
                Boolean.TRUE, new org.xwiki.rest.model.jaxb.Object());

            // Assert: rama Boolean.TRUE.equals(minorRevision) == true
            assertThat(respuesta.getStatus(), is(Response.Status.ACCEPTED.getStatusCode()));
            verify(doc).save("", true);                                       // MOCK verificado
        }
    }

    @Test
    void updateObject_WhenSaveFails_ShouldThrowXWikiRestException() throws Exception
    {
        // Arrange (fault injection): el guardado del documento revienta
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        when(this.authorization.hasAccess(Right.EDIT, DOC_REFERENCE)).thenReturn(true);
        when(doc.getObject(CLASS_NAME, NUMERO)).thenReturn(mock(com.xpn.xwiki.api.Object.class));
        doThrow(new XWikiException()).when(doc).save(anyString(), anyBoolean()); // STUB de falla

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            XWikiRestException ex = assertThrows(XWikiRestException.class,
                () -> this.objectResource.updateObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO, false,
                    new org.xwiki.rest.model.jaxb.Object()));
            assertThat(ex.getCause(), is(instanceOf(XWikiException.class)));
        }
    }

    /* ------------------------------------------------------------------ */
    /* deleteObject                                                         */
    /* ------------------------------------------------------------------ */

    @Test
    void deleteObject_WhenUserHasNoEditRight_ShouldThrowUnauthorized() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        when(this.authorization.hasAccess(Right.EDIT, DOC_REFERENCE)).thenReturn(false); // STUB

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.objectResource.deleteObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO));
            assertThat(ex.getResponse().getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
            verify(doc, never()).removeObject(any());                         // no se borro nada
            verify(doc, never()).save();
        }
    }

    @Test
    void deleteObject_WhenObjectDoesNotExist_ShouldThrowNotFound() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        when(this.authorization.hasAccess(Right.EDIT, DOC_REFERENCE)).thenReturn(true);
        when(doc.getObject(CLASS_NAME, NUMERO)).thenReturn(null);             // STUB: no existe

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.objectResource.deleteObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO));
            assertThat(ex.getResponse().getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
            verify(doc, never()).removeObject(any());
            verify(doc, never()).save();
        }
    }

    @Test
    void deleteObject_WhenValid_ShouldRemoveObjectAndSave() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        when(this.authorization.hasAccess(Right.EDIT, DOC_REFERENCE)).thenReturn(true);

        com.xpn.xwiki.api.Object xwikiObject = mock(com.xpn.xwiki.api.Object.class);    // MOCK
        when(doc.getObject(CLASS_NAME, NUMERO)).thenReturn(xwikiObject);      // STUB

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act
            this.objectResource.deleteObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO);

            // Assert: primero se quita el objeto y despues se guarda el documento
            InOrder orden = inOrder(doc);                                     // MOCK verificado en orden
            orden.verify(doc).removeObject(same(xwikiObject));
            orden.verify(doc).save();
        }
    }

    @Test
    void deleteObject_WhenSaveFails_ShouldThrowXWikiRestException() throws Exception
    {
        // Arrange (fault injection): el guardado tras el borrado revienta
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        when(this.authorization.hasAccess(Right.EDIT, DOC_REFERENCE)).thenReturn(true);
        when(doc.getObject(CLASS_NAME, NUMERO)).thenReturn(mock(com.xpn.xwiki.api.Object.class));
        doThrow(new XWikiException()).when(doc).save();                       // STUB de falla

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            XWikiRestException ex = assertThrows(XWikiRestException.class,
                () -> this.objectResource.deleteObject(WIKI, SPACE, PAGE, CLASS_NAME, NUMERO));
            assertThat(ex.getCause(), is(instanceOf(XWikiException.class)));
        }
    }
}
