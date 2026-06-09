package org.xwiki.rest.internal.resources.pages;

import java.util.Arrays;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Tags;
import org.xwiki.test.junit5.mockito.ComponentTest;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
@SuppressWarnings("deprecation")
class PageTagsResourceImplSetSuccessTest extends AbstractPageTagsResourceTest
{
    @Test
    void setTags_WhenTagObjectExists_ShouldCallSaveDocument_UsingMock() throws Exception
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

        BaseObject tagObject = mock(BaseObject.class);
        when(xwikiDoc.getObject(TAG_CLASS, 0)).thenReturn(tagObject);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            utils.when(() -> Utils.getXWikiUser(any())).thenReturn("XWiki.Admin");

            Response response = this.resource.setTags(WIKI, SPACE, PAGE, true, tags);

            verify(xwiki).saveDocument(xwikiDoc, "", true, xcontext);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        }
    }

    @Test
    void setTags_WhenTagObjectDoesNotExist_ShouldCreateNewObject_UsingFake() throws Exception
    {
        FakeTags fakeTags = new FakeTags("java", "testing");

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

        BaseObject newTagObject = mock(BaseObject.class);
        when(xwikiDoc.getObject(TAG_CLASS, 0)).thenReturn(null);
        when(xwikiDoc.createNewObject(TAG_CLASS, xcontext)).thenReturn(5);
        when(xwikiDoc.getObject(TAG_CLASS, 5)).thenReturn(newTagObject);
        when(newTagObject.getClassName()).thenReturn(TAG_CLASS);

        BaseClass baseClass = mock(BaseClass.class);
        when(xwiki.getClass(TAG_CLASS, xcontext)).thenReturn(baseClass);
        when(baseClass.getPropertyNames()).thenReturn(new String[]{});

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getXWikiApi(any())).thenReturn(apiXWiki);
            utils.when(() -> Utils.getXWikiUser(any())).thenReturn("XWiki.Admin");

            Response response = this.resource.setTags(WIKI, SPACE, PAGE, false, fakeTags);

            verify(xwikiDoc).createNewObject(TAG_CLASS, xcontext);
            verify(newTagObject).set("tags", Arrays.asList("java", "testing"), xcontext);
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        }
    }
}
