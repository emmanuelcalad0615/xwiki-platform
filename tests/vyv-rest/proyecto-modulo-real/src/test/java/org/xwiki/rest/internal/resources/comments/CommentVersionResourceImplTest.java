/*
 * Pruebas unitarias automatizadas de CommentVersionResourceImpl (un comentario en una
 * version de la pagina).
 * Patron AAA + principios FIRST. Dobles: Mock, Stub, Dummy.
 *
 * Ejecutar: copiar a
 *   xwiki-platform-rest-server/src/test/java/org/xwiki/rest/internal/resources/comments/
 *   mvn test -pl :xwiki-platform-rest-server -am -Dtest=CommentVersionResourceImplTest
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link CommentVersionResourceImpl}.
 */
@ComponentTest
class CommentVersionResourceImplTest
{
    private static final String WIKI = "xwiki";

    private static final String SPACE = "Main";

    private static final String PAGE = "PruebaComentarios";

    private static final String VERSION = "1.1";

    @InjectMockComponents
    private CommentVersionResourceImpl resource;

    private UriInfo uriInfo;   // MOCK

    @BeforeEach
    void setUp() throws Exception
    {
        this.uriInfo = mock(UriInfo.class);
        when(this.uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/rest/"));
        FieldUtils.writeField(this.resource, "uriInfo", this.uriInfo, true);
    }

    private Document mockVersionedDocument(com.xpn.xwiki.api.XWiki apiXWiki) throws XWikiException
    {
        Document doc = mock(Document.class);                                  // MOCK
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenReturn(doc); // STUB
        when(doc.isNew()).thenReturn(false);                                  // STUB
        when(doc.getDocumentRevision(anyString())).thenReturn(doc);          // STUB
        return doc;
    }

    @Test
    void getCommentVersion_WhenIdExists_ShouldReturnComment() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockVersionedDocument(apiXWiki);
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
            Comment result = this.resource.getCommentVersion(WIKI, SPACE, PAGE, VERSION, 0, 0, 10, dummyPrettyNames);

            // Assert
            assertThat(result, is(notNullValue()));
            assertThat(result.getId(), is(0));
        }
    }

    @Test
    void getCommentVersion_WhenIdNotFound_ShouldThrowNotFound() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockVersionedDocument(apiXWiki);
        com.xpn.xwiki.api.Object existing = mock(com.xpn.xwiki.api.Object.class);
        when(existing.getNumber()).thenReturn(0);

        Vector<com.xpn.xwiki.api.Object> comments = new Vector<>();
        comments.add(existing);
        when(doc.getComments()).thenReturn(comments);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.resource.getCommentVersion(WIKI, SPACE, PAGE, VERSION, 9999, 0, 10, false));
            assertThat(ex.getResponse().getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    @Test
    void getCommentVersion_WhenDocumentFails_ShouldThrowXWikiRestException() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenThrow(new XWikiException());

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            assertThrows(XWikiRestException.class,
                () -> this.resource.getCommentVersion(WIKI, SPACE, PAGE, VERSION, 0, 0, 10, false));
        }
    }
}
