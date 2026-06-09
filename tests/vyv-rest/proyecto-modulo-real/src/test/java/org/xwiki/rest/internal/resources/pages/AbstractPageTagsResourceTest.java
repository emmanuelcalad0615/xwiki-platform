package org.xwiki.rest.internal.resources.pages;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Tag;
import org.xwiki.rest.model.jaxb.Tags;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Clase base para los tests de PageTagsResourceImpl.
 * Proporciona constantes y helpers para evitar duplicacion de codigo.
 */
public abstract class AbstractPageTagsResourceTest
{
    protected static final String WIKI = "xwiki";
    protected static final String SPACE = "Main";
    protected static final String PAGE = "TestPageTags";
    protected static final String PAGE_ID = "xwiki:Main.TestPageTags";
    protected static final String TAG_CLASS = "XWiki.TagClass";

    @InjectMockComponents
    protected PageTagsResourceImpl resource;

    protected UriInfo uriInfo;

    @BeforeEach
    void setUp() throws Exception
    {
        this.uriInfo = mock(UriInfo.class);
        when(this.uriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/rest/"));
        FieldUtils.writeField(this.resource, "uriInfo", this.uriInfo, true);
    }

    protected Document mockAccessibleDocument(com.xpn.xwiki.api.XWiki apiXWiki) throws XWikiException
    {
        Document doc = mock(Document.class);
        when(apiXWiki.getDocument(any(DocumentReference.class))).thenReturn(doc);
        when(doc.isNew()).thenReturn(false);
        when(doc.getLocked()).thenReturn(false);
        return doc;
    }

    @SuppressWarnings("unchecked")
    protected XWikiContext injectMockedXWikiContext() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        Provider<XWikiContext> provider = mock(Provider.class);
        when(provider.get()).thenReturn(xcontext);
        FieldUtils.writeField(this.resource, "xcontextProvider", provider, true);
        return xcontext;
    }

    static class FakeTags extends Tags
    {
        private final List<String> tagNames;

        FakeTags(String... names)
        {
            this.tagNames = Arrays.asList(names);
        }

        @Override
        public List<Tag> getTags()
        {
            List<Tag> list = new ArrayList<>();
            for (String name : tagNames) {
                Tag t = new Tag();
                t.setName(name);
                list.add(t);
            }
            return list;
        }
    }
}
