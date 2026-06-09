package org.xwiki.rest.internal.resources.pages;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Tags;
import org.xwiki.test.junit5.mockito.ComponentTest;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
@SuppressWarnings("deprecation")
class PageTagsResourceImplSetErrorTest extends AbstractPageTagsResourceTest
{
    @Test
    void setTags_WhenDocumentIsNull_ShouldThrowUnauthorized_UsingDummy() throws Exception
    {
        Tags dummyTags = new Tags();
        Boolean dummyMinor = false;

        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenReturn(null);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.resource.setTags(WIKI, SPACE, PAGE, dummyMinor, dummyTags));
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), ex.getResponse().getStatus());
        }
    }

    @Test
    void setTags_WhenUserHasNoEditRights_ShouldThrowUnauthorized_UsingStub() throws Exception
    {
        Tags dummyTags = new Tags();

        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        when(doc.hasAccessLevel(eq("edit"), anyString())).thenReturn(false);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            utils.when(() -> Utils.getXWikiUser(any())).thenReturn("XWiki.Invitado");

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.resource.setTags(WIKI, SPACE, PAGE, false, dummyTags));
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), ex.getResponse().getStatus());
        }
    }

    @Test
    void setTags_WhenCreateNewObjectFails_ShouldThrowInternalServerError_UsingMock() throws Exception
    {
        Tags tags = new Tags();

        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        when(doc.hasAccessLevel(eq("edit"), anyString())).thenReturn(true);
        DocumentReference docRef = mock(DocumentReference.class);
        when(doc.getDocumentReference()).thenReturn(docRef);

        XWikiContext xcontext = injectMockedXWikiContext();
        XWiki xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        XWikiDocument xwikiDoc = mock(XWikiDocument.class);
        when(xwikiDoc.clone()).thenReturn(xwikiDoc);
        when(xwiki.getDocument(docRef, xcontext)).thenReturn(xwikiDoc);

        when(xwikiDoc.getObject(TAG_CLASS, 0)).thenReturn(null);
        when(xwikiDoc.createNewObject(TAG_CLASS, xcontext)).thenReturn(0);
        when(xwikiDoc.getObject(TAG_CLASS, 0)).thenReturn(null);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            utils.when(() -> Utils.getXWikiUser(any())).thenReturn("XWiki.Admin");

            WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> this.resource.setTags(WIKI, SPACE, PAGE, false, tags));
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ex.getResponse().getStatus());
        }
    }

    @Test
    void setTags_WhenGetDocumentThrows_ShouldThrowXWikiRestException_UsingSpy() throws Exception
    {
        Tags tags = new Tags();

        com.xpn.xwiki.api.XWiki apiXWiki = mock(com.xpn.xwiki.api.XWiki.class);
        Document doc = mockAccessibleDocument(apiXWiki);
        when(doc.hasAccessLevel(eq("edit"), anyString())).thenReturn(true);
        DocumentReference docRef = mock(DocumentReference.class);
        when(doc.getDocumentReference()).thenReturn(docRef);

        XWikiContext xcontext = injectMockedXWikiContext();
        XWiki xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        when(xwiki.getDocument(docRef, xcontext)).thenThrow(new XWikiException());

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            utils.when(() -> Utils.getXWikiUser(any())).thenReturn("XWiki.Admin");

            assertThrows(XWikiRestException.class, () -> this.resource.setTags(WIKI, SPACE, PAGE, false, tags));
            verify(xwiki).getDocument(docRef, xcontext);
        }
    }
}
