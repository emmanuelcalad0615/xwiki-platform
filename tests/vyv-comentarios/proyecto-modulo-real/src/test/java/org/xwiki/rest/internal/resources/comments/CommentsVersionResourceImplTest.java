/*
 * Pruebas unitarias automatizadas de CommentsVersionResourceImpl (historial: lista de
 * comentarios en una version de la pagina).
 * Patron AAA + principios FIRST. Dobles: Mock, Stub, Spy, Fake, Dummy.
 * Cubre caminos backend 14-16.
 *
 * Ejecutar: copiar a
 *   xwiki-platform-rest-server/src/test/java/org/xwiki/rest/internal/resources/comments/
 *   mvn test -pl :xwiki-platform-rest-server -am -Dtest=CommentsVersionResourceImplTest
 */
package org.xwiki.rest.internal.resources.comments;

import java.net.URI;
import java.util.Vector;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link CommentsVersionResourceImpl}.
 */
@ComponentTest
class CommentsVersionResourceImplTest
{
    private static final String WIKI = "xwiki";

    private static final String SPACE = "Main";

    private static final String PAGE = "PruebaComentarios";

    private static final String VERSION = "1.1";

    @InjectMockComponents
    private CommentsVersionResourceImpl resource;

    private UriInfo uriInfo;   // MOCK

    @BeforeEach
    void setUp() throws Exception
    {
        this.uriInfo = mock(UriInfo.class);
        when(this.uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/rest/"));
        FieldUtils.writeField(this.resource, "uriInfo", this.uriInfo, true);
    }

    /** Documento accesible en una version concreta. */
    private Document mockVersionedDocument(com.xpn.xwiki.api.XWiki apiXWiki) throws XWikiException
    {
        Document doc = mock(Document.class);                                  // MOCK
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenReturn(doc); // STUB
        when(doc.isNew()).thenReturn(false);                                  // STUB
        when(doc.getDocumentRevision(anyString())).thenReturn(doc);          // STUB: version solicitada
        return doc;
    }

    @Test
    void getCommentsVersion_WhenNoComments_ShouldReturnEmptyList() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockVersionedDocument(apiXWiki);
        when(doc.getComments()).thenReturn(new Vector<>());
        Boolean dummyPrettyNames = Boolean.FALSE;                            // DUMMY

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act
            Comments result = this.resource.getCommentsVersion(WIKI, SPACE, PAGE, VERSION, 0, 10, dummyPrettyNames);

            // Assert
            assertNotNull(result);
            assertTrue(result.getComments().isEmpty());
        }
    }

    @Test
    void getCommentsVersion_WhenHasComments_ShouldReturnThem() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockVersionedDocument(apiXWiki);
        com.xpn.xwiki.api.Object c0 = mock(com.xpn.xwiki.api.Object.class);   // MOCK

        // FAKE: almacen en memoria de la version.
        Vector<com.xpn.xwiki.api.Object> fakeStore = new Vector<>();
        fakeStore.add(c0);
        when(doc.getComments()).thenReturn(fakeStore);

        ObjectFactory spyFactory = spy(new ObjectFactory());                 // SPY
        FieldUtils.writeField(this.resource, "objectFactory", spyFactory, true);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class);
             MockedStatic<DomainObjectFactory> dof = mockStatic(DomainObjectFactory.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            dof.when(() -> DomainObjectFactory.createComment(any(), any(), any(), any(), any(), any()))
                .thenReturn(new Comment());

            // Act
            Comments result = this.resource.getCommentsVersion(WIKI, SPACE, PAGE, VERSION, 0, 10, false);

            // Assert
            assertEquals(1, result.getComments().size());
            verify(spyFactory).createComments();                            // SPY
        }
    }

    @Test
    void getCommentsVersion_WhenDocumentFails_ShouldThrowXWikiRestException() throws Exception
    {
        // Arrange
        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenThrow(new XWikiException());

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            // Act + Assert
            assertThrows(XWikiRestException.class,
                () -> this.resource.getCommentsVersion(WIKI, SPACE, PAGE, VERSION, 0, 10, false));
        }
    }
}
