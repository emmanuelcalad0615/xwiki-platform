/*
 * Pruebas unitarias automatizadas de CommentsResourceImpl (listar + crear).
 * Patron AAA + principios FIRST + los 5 tipos de dobles: Dummy, Fake, Stub, Spy, Mock.
 * Cubre caminos backend 1-9.
 *
 * Ejecutar: copiar a
 *   xwiki-platform-rest-server/src/test/java/org/xwiki/rest/internal/resources/comments/
 *   mvn test -pl :xwiki-platform-rest-server -am -Dtest=CommentsResourceImplTest
 */
package org.xwiki.rest.internal.resources.comments;

import java.net.URI;
import java.util.Vector;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.rest.model.jaxb.Comments;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link CommentsResourceImpl}.
 */
@ComponentTest
class CommentsResourceImplTest
{
    private static final String WIKI = "xwiki";

    private static final String SPACE = "Main";

    private static final String PAGE = "PruebaComentarios";

    private static final String COMMENTS_CLASS = "XWiki.XWikiComments";

    @InjectMockComponents
    private CommentsResourceImpl commentsResource;

    /** MOCK: doble completo de UriInfo (campo protegido del recurso). */
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception
    {
        this.uriInfo = mock(UriInfo.class);
        when(this.uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/rest/"));
        FieldUtils.writeField(this.commentsResource, "uriInfo", this.uriInfo, true);
    }

    /** FAKE: almacen de comentarios en memoria (implementacion funcional, no un mock). */
    static final class FakeCommentRepository
    {
        private final Vector<com.xpn.xwiki.api.Object> store = new Vector<>();

        void save(com.xpn.xwiki.api.Object comment)
        {
            this.store.add(comment);
        }

        Vector<com.xpn.xwiki.api.Object> findAll()
        {
            return this.store;
        }
    }

    /** Helper: arma el mock del documento accesible. */
    private Document mockAccessibleDocument(com.xpn.xwiki.api.XWiki apiXWiki) throws XWikiException
    {
        Document doc = mock(Document.class);                                  // MOCK
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenReturn(doc); // STUB
        when(doc.isNew()).thenReturn(false);                                  // STUB
        when(doc.getLocked()).thenReturn(false);                             // STUB
        return doc;
    }

    // ===================== LISTAR (caminos 1-3) =====================

    @Test
    void getComments_WhenPageHasNoComments_ShouldReturnEmptyList() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class); // MOCK
        Document doc = mockAccessibleDocument(apiXWiki);
        when(doc.getComments()).thenReturn(new Vector<>());                   // STUB
        Boolean dummyPrettyNames = Boolean.FALSE;                            // DUMMY

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act
            Comments result = this.commentsResource.getComments(WIKI, SPACE, PAGE, 0, 10, dummyPrettyNames);

            // Assert
            assertThat(result, is(notNullValue()));
            assertThat(result.getComments(), is(empty()));
        }
    }

    @Test
    void getComments_WhenPageHasComments_ShouldReturnThem() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        com.xpn.xwiki.api.Object c0 = mock(com.xpn.xwiki.api.Object.class);   // MOCK
        com.xpn.xwiki.api.Object c1 = mock(com.xpn.xwiki.api.Object.class);   // MOCK

        FakeCommentRepository fakeRepo = new FakeCommentRepository();         // FAKE
        fakeRepo.save(c0);
        fakeRepo.save(c1);
        when(doc.getComments()).thenReturn(fakeRepo.findAll());

        ObjectFactory spyFactory = spy(new ObjectFactory());                 // SPY
        FieldUtils.writeField(this.commentsResource, "objectFactory", spyFactory, true);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<DomainObjectFactory> dof = mockStatic(DomainObjectFactory.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            dof.when(() -> DomainObjectFactory.createComment(any(), any(), any(), any(), any(), any()))
                .thenReturn(new Comment(), new Comment());

            // Act
            Comments result = this.commentsResource.getComments(WIKI, SPACE, PAGE, 0, 10, false);

            // Assert
            assertThat(result.getComments(), hasSize(2));
            verify(spyFactory).createComments();                            // SPY: objeto real usado
        }
    }

    @Test
    void getComments_WhenDocumentFails_ShouldThrowXWikiRestException() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenThrow(new XWikiException()); // STUB

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            assertThrows(XWikiRestException.class,
                () -> this.commentsResource.getComments(WIKI, SPACE, PAGE, 0, 10, false));
        }
    }

    // ===================== CREAR (caminos 4-9) =====================

    @Test
    void postComment_WhenUserHasNoRights_ShouldThrowUnauthorized() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenReturn(null); // STUB: sin derechos

        Comment input = new Comment();
        input.setText("Sin permiso");

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.commentsResource.postComment(WIKI, SPACE, PAGE, input));
            assertThat(ex.getResponse().getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
        }
    }

    @Test
    void postComment_WhenHighlightTextAndReplyTo_ShouldCreateAndReturn201() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        com.xpn.xwiki.api.Object commentObject = mock(com.xpn.xwiki.api.Object.class); // MOCK
        when(doc.createNewObject(COMMENTS_CLASS)).thenReturn(0);
        when(doc.getObject(COMMENTS_CLASS, 0)).thenReturn(commentObject);

        Comment input = new Comment();
        input.setHighlight("fragmento");
        input.setText("Respuesta resaltada");
        input.setReplyTo(0);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<DomainObjectFactory> dof = mockStatic(DomainObjectFactory.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            utils.when(() -> Utils.getXWikiUser(any())).thenReturn("XWiki.Admin");
            utils.when(() -> Utils.createURI(any(), any(), any(), any(), any(), any()))
                .thenReturn(URI.create("http://localhost:8080/rest/comments/0"));
            dof.when(() -> DomainObjectFactory.createComment(any(), any(), any(), any(), any(), any()))
                .thenReturn(new Comment());

            // Act
            Response response = this.commentsResource.postComment(WIKI, SPACE, PAGE, input);

            // Assert
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            verify(commentObject).set(eq("highlight"), eq("fragmento"));
            verify(commentObject).set(eq("comment"), eq("Respuesta resaltada"));
            verify(commentObject).set(eq("replyto"), eq(0));
            verify(doc).save();
        }
    }

    @Test
    void postComment_WhenOnlyHighlight_ShouldCreateAndReturn201() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        com.xpn.xwiki.api.Object commentObject = mock(com.xpn.xwiki.api.Object.class);
        when(doc.createNewObject(COMMENTS_CLASS)).thenReturn(0);
        when(doc.getObject(COMMENTS_CLASS, 0)).thenReturn(commentObject);

        Comment input = new Comment();
        input.setHighlight("solo-resaltado");

        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<DomainObjectFactory> dof = mockStatic(DomainObjectFactory.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            utils.when(() -> Utils.getXWikiUser(any())).thenReturn("XWiki.Admin");
            utils.when(() -> Utils.createURI(any(), any(), any(), any(), any(), any()))
                .thenReturn(URI.create("http://localhost:8080/rest/comments/0"));
            dof.when(() -> DomainObjectFactory.createComment(any(), any(), any(), any(), any(), any()))
                .thenReturn(new Comment());

            // Act
            Response response = this.commentsResource.postComment(WIKI, SPACE, PAGE, input);

            // Assert
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            verify(commentObject).set(eq("highlight"), eq("solo-resaltado"));
            verify(commentObject, never()).set(eq("comment"), any());
            verify(doc).save();
        }
    }

    @Test
    void postComment_WhenBodyHasText_ShouldCreateAndReturn201() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        com.xpn.xwiki.api.Object commentObject = mock(com.xpn.xwiki.api.Object.class);
        when(doc.createNewObject(COMMENTS_CLASS)).thenReturn(0);
        when(doc.getObject(COMMENTS_CLASS, 0)).thenReturn(commentObject);

        Comment input = new Comment();
        input.setText("Comentario de prueba");

        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<DomainObjectFactory> dof = mockStatic(DomainObjectFactory.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            utils.when(() -> Utils.getXWikiUser(any())).thenReturn("XWiki.Admin");
            utils.when(() -> Utils.createURI(any(), any(), any(), any(), any(), any()))
                .thenReturn(URI.create("http://localhost:8080/rest/comments/0"));
            dof.when(() -> DomainObjectFactory.createComment(any(), any(), any(), any(), any(), any()))
                .thenReturn(new Comment());

            // Act
            Response response = this.commentsResource.postComment(WIKI, SPACE, PAGE, input);

            // Assert
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            verify(commentObject).set(eq("comment"), eq("Comentario de prueba"));
            verify(doc).save();
        }
    }

    @Test
    void postComment_WhenTextAndReplyTo_ShouldCreateReply201() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        com.xpn.xwiki.api.Object commentObject = mock(com.xpn.xwiki.api.Object.class);
        when(doc.createNewObject(COMMENTS_CLASS)).thenReturn(1);
        when(doc.getObject(COMMENTS_CLASS, 1)).thenReturn(commentObject);

        Comment input = new Comment();
        input.setText("Respuesta");
        input.setReplyTo(0);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<DomainObjectFactory> dof = mockStatic(DomainObjectFactory.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            utils.when(() -> Utils.getXWikiUser(any())).thenReturn("XWiki.Admin");
            utils.when(() -> Utils.createURI(any(), any(), any(), any(), any(), any()))
                .thenReturn(URI.create("http://localhost:8080/rest/comments/1"));
            dof.when(() -> DomainObjectFactory.createComment(any(), any(), any(), any(), any(), any()))
                .thenReturn(new Comment());

            // Act
            Response response = this.commentsResource.postComment(WIKI, SPACE, PAGE, input);

            // Assert
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            verify(commentObject).set(eq("comment"), eq("Respuesta"));
            verify(commentObject).set(eq("replyto"), eq(0));
            verify(doc).save();
        }
    }

    @Test
    void postComment_WhenBodyHasNoContent_ShouldReturnNullWithoutSaving() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        com.xpn.xwiki.api.Object commentObject = mock(com.xpn.xwiki.api.Object.class);
        when(doc.createNewObject(COMMENTS_CLASS)).thenReturn(0);
        when(doc.getObject(COMMENTS_CLASS, 0)).thenReturn(commentObject);

        Comment input = new Comment();
        input.setReplyTo(0);   // sin text ni highlight

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            utils.when(() -> Utils.getXWikiUser(any())).thenReturn("XWiki.Admin");

            // Act
            Response response = this.commentsResource.postComment(WIKI, SPACE, PAGE, input);

            // Assert
            assertThat(response, is(nullValue()));
            verify(doc, never()).save();
        }
    }
}
