/*
 * Pruebas unitarias automatizadas de CommentResourceImpl (obtener un comentario).
 * Patron AAA + principios FIRST. Dobles: Mock, Stub, Spy, Dummy.
 * Cubre caminos backend 10-13.
 *
 * Ejecutar: copiar a
 *   xwiki-platform-rest-server/src/test/java/org/xwiki/rest/internal/resources/comments/
 *   mvn test -pl :xwiki-platform-rest-server -am -Dtest=CommentResourceImplTest
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link CommentResourceImpl}.
 */
@ComponentTest
class CommentResourceImplTest
{
    private static final String WIKI = "xwiki";

    private static final String SPACE = "Main";

    private static final String PAGE = "PruebaComentarios";

    @InjectMockComponents
    private CommentResourceImpl commentResource;

    /** MOCK: doble de UriInfo. */
    private UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception
    {
        this.uriInfo = mock(UriInfo.class);
        when(this.uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/rest/"));
        FieldUtils.writeField(this.commentResource, "uriInfo", this.uriInfo, true);
    }

    private Document mockAccessibleDocument(com.xpn.xwiki.api.XWiki apiXWiki) throws XWikiException
    {
        Document doc = mock(Document.class);                                  // MOCK
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenReturn(doc); // STUB
        when(doc.isNew()).thenReturn(false);                                  // STUB
        return doc;
    }

    @Test
    void getComment_WhenIdExistsFirst_ShouldReturnComment() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        com.xpn.xwiki.api.Object existing = mock(com.xpn.xwiki.api.Object.class); // MOCK
        when(existing.getNumber()).thenReturn(0);                            // STUB

        Vector<com.xpn.xwiki.api.Object> comments = new Vector<>();
        comments.add(existing);
        when(doc.getComments()).thenReturn(comments);
        Boolean dummyPrettyNames = Boolean.FALSE;                            // DUMMY

        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<DomainObjectFactory> dof = mockStatic(DomainObjectFactory.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            Comment expected = new Comment();
            expected.setId(0);
            dof.when(() -> DomainObjectFactory.createComment(any(), any(), any(), any(), any(), any()))
                .thenReturn(expected);

            // Act
            Comment result = this.commentResource.getComment(WIKI, SPACE, PAGE, 0, 0, 10, dummyPrettyNames);

            // Assert
            assertThat(result, is(notNullValue()));
            assertThat(result.getId(), is(0));
        }
    }

    @Test
    void getComment_WhenIdInSecondPosition_ShouldIterateAndReturnIt() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        com.xpn.xwiki.api.Object c0 = mock(com.xpn.xwiki.api.Object.class);   // MOCK
        com.xpn.xwiki.api.Object c1 = mock(com.xpn.xwiki.api.Object.class);   // MOCK
        when(c0.getNumber()).thenReturn(0);                                  // STUB
        when(c1.getNumber()).thenReturn(1);                                  // STUB

        // SPY: vector real espiado para comprobar que el bucle lo recorre.
        Vector<com.xpn.xwiki.api.Object> spyComments = spy(new Vector<>());
        spyComments.add(c0);
        spyComments.add(c1);
        when(doc.getComments()).thenReturn(spyComments);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<DomainObjectFactory> dof = mockStatic(DomainObjectFactory.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            Comment expected = new Comment();
            expected.setId(1);
            dof.when(() -> DomainObjectFactory.createComment(any(), any(), any(), any(), any(), any()))
                .thenReturn(expected);

            // Act
            Comment result = this.commentResource.getComment(WIKI, SPACE, PAGE, 1, 0, 10, false);

            // Assert
            assertThat(result.getId(), is(1));
            verify(spyComments, atLeastOnce()).iterator();                  // SPY: se itero la coleccion
        }
    }

    @Test
    void getComment_WhenIdNotFound_ShouldThrowNotFound() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        com.xpn.xwiki.api.Object existing = mock(com.xpn.xwiki.api.Object.class);
        when(existing.getNumber()).thenReturn(0);

        Vector<com.xpn.xwiki.api.Object> comments = new Vector<>();
        comments.add(existing);
        when(doc.getComments()).thenReturn(comments);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.commentResource.getComment(WIKI, SPACE, PAGE, 9999, 0, 10, false));
            assertThat(ex.getResponse().getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    void getComment_WhenDocumentFails_ShouldThrowXWikiRestException() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenThrow(new XWikiException());

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            assertThrows(XWikiRestException.class,
                () -> this.commentResource.getComment(WIKI, SPACE, PAGE, 0, 0, 10, false));
        }
    }
}
