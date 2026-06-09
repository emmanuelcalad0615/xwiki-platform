package org.xwiki.rest.internal.resources.pages;

import java.net.URI;
import java.util.Arrays;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.ObjectFactory;
import org.xwiki.rest.model.jaxb.Tags;
import org.xwiki.test.junit5.mockito.ComponentTest;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
@SuppressWarnings("deprecation")
class PageTagsResourceImplGetSuccessTest extends AbstractPageTagsResourceTest
{
    @Test
    @SuppressWarnings("unchecked")
    void getPageTags_ShouldUseObjectFactory_WhenDocumentHasTags_UsingSpy() throws Exception
    {
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiDocument xwikiDoc = mock(XWikiDocument.class);
        BaseObject tagObject = mock(BaseObject.class);
        BaseProperty tagProperty = mock(BaseProperty.class);

        when(xwikiDoc.getObject(TAG_CLASS)).thenReturn(tagObject);
        when(tagObject.safeget("tags")).thenReturn(tagProperty);
        when(tagProperty.getValue()).thenReturn(Arrays.asList("etiqueta1"));
        when(xwiki.getDocument(PAGE_ID, xcontext)).thenReturn(xwikiDoc);

        ObjectFactory spyFactory = spy(new ObjectFactory());
        FieldUtils.writeField(this.resource, "objectFactory", spyFactory, true);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getPageId(anyString(), anyList(), anyString())).thenReturn(PAGE_ID);
            utils.when(() -> Utils.getXWiki(any())).thenReturn(xwiki);
            utils.when(() -> Utils.getXWikiContext(any())).thenReturn(xcontext);
            utils.when(() -> Utils.createURI(any(), any(), any(Object[].class)))
                .thenAnswer(inv -> URI.create("http://localhost:8080/rest/wikis/xwiki/tags/etiqueta1"));

            Tags result = this.resource.getPageTags(WIKI, SPACE, PAGE);

            assertNotNull(result);
            verify(spyFactory).createTags();
            verify(spyFactory).createTag();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPageTags_WhenGetDocumentThrows_ShouldThrowXWikiRestException_UsingMock() throws Exception
    {
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);

        when(xwiki.getDocument(anyString(), eq(xcontext))).thenThrow(new XWikiException());

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getPageId(anyString(), anyList(), anyString())).thenReturn(PAGE_ID);
            utils.when(() -> Utils.getXWiki(any())).thenReturn(xwiki);
            utils.when(() -> Utils.getXWikiContext(any())).thenReturn(xcontext);

            assertThrows(XWikiRestException.class, () -> this.resource.getPageTags(WIKI, SPACE, PAGE));
        }
    }
}
