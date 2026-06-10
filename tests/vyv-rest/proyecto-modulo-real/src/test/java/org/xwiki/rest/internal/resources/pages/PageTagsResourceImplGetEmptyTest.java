package org.xwiki.rest.internal.resources.pages;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.xwiki.rest.model.jaxb.Tags;
import org.xwiki.rest.internal.Utils;
import org.xwiki.test.junit5.mockito.ComponentTest;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ComponentTest
@SuppressWarnings("deprecation")
class PageTagsResourceImplGetEmptyTest extends AbstractPageTagsResourceTest
{
    @Test
    @SuppressWarnings("unchecked")
    void getPageTags_WhenDocumentHasNoTagObject_ShouldReturnEmptyTags_UsingStub() throws Exception
    {
        // Arrange
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiDocument xwikiDoc = mock(XWikiDocument.class);

        when(xwikiDoc.getObject(TAG_CLASS)).thenReturn(null);
        when(xwiki.getDocument(PAGE_ID, xcontext)).thenReturn(xwikiDoc);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getPageId(anyString(), anyList(), anyString())).thenReturn(PAGE_ID);
            utils.when(() -> Utils.getXWiki(any())).thenReturn(xwiki);
            utils.when(() -> Utils.getXWikiContext(any())).thenReturn(xcontext);

            // Act
            Tags result = this.resource.getPageTags(WIKI, SPACE, PAGE);

            // Assert
            assertThat(result, is(notNullValue()));
            assertThat(result.getTags(), is(empty()));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPageTags_WhenTagPropertyIsNull_ShouldReturnEmptyTags_UsingStub() throws Exception
    {
        // Arrange
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiDocument xwikiDoc = mock(XWikiDocument.class);
        BaseObject tagObject = mock(BaseObject.class);

        when(xwikiDoc.getObject(TAG_CLASS)).thenReturn(tagObject);
        when(tagObject.safeget("tags")).thenReturn(null);
        when(xwiki.getDocument(PAGE_ID, xcontext)).thenReturn(xwikiDoc);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getPageId(anyString(), anyList(), anyString())).thenReturn(PAGE_ID);
            utils.when(() -> Utils.getXWiki(any())).thenReturn(xwiki);
            utils.when(() -> Utils.getXWikiContext(any())).thenReturn(xcontext);

            // Act
            Tags result = this.resource.getPageTags(WIKI, SPACE, PAGE);

            // Assert
            assertThat(result, is(notNullValue()));
            assertThat(result.getTags(), is(empty()));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPageTags_WhenTagValueIsNull_ShouldReturnEmptyTags_UsingStub() throws Exception
    {
        // Arrange
        XWiki xwiki = mock(XWiki.class);
        XWikiContext xcontext = mock(XWikiContext.class);
        XWikiDocument xwikiDoc = mock(XWikiDocument.class);
        BaseObject tagObject = mock(BaseObject.class);
        BaseProperty tagProperty = mock(BaseProperty.class);

        when(xwikiDoc.getObject(TAG_CLASS)).thenReturn(tagObject);
        when(tagObject.safeget("tags")).thenReturn(tagProperty);
        when(tagProperty.getValue()).thenReturn(null);
        when(xwiki.getDocument(PAGE_ID, xcontext)).thenReturn(xwikiDoc);

        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            utils.when(() -> Utils.getPageId(anyString(), anyList(), anyString())).thenReturn(PAGE_ID);
            utils.when(() -> Utils.getXWiki(any())).thenReturn(xwiki);
            utils.when(() -> Utils.getXWikiContext(any())).thenReturn(xcontext);

            // Act
            Tags result = this.resource.getPageTags(WIKI, SPACE, PAGE);

            // Assert
            assertThat(result, is(notNullValue()));
            assertThat(result.getTags(), is(empty()));
        }
    }
}
