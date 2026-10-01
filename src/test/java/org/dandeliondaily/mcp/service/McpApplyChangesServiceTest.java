package org.dandeliondaily.mcp.service;

import org.junit.Assert;
import org.junit.Test;

public class McpApplyChangesServiceTest {

    @Test
    public void acceptsHttpAndHttpsLinks() {
        Assert.assertNull(McpApplyChangesService.linkUrlError("https://github.com/org/repo/pull/12"));
        Assert.assertNull(McpApplyChangesService.linkUrlError("http://example.org/doc?id=3#top"));
    }

    @Test
    public void rejectsOtherSchemesAndRelativeLinks() {
        Assert.assertNotNull(McpApplyChangesService.linkUrlError("javascript:alert(1)"));
        Assert.assertNotNull(McpApplyChangesService.linkUrlError("ftp://example.org/file"));
        Assert.assertNotNull(McpApplyChangesService.linkUrlError("/relative/path"));
        Assert.assertNotNull(McpApplyChangesService.linkUrlError("not a url"));
    }

    @Test
    public void rejectsLinksLongerThanTheColumn() {
        StringBuilder url = new StringBuilder("https://example.org/");
        while (url.length() <= 1200) {
            url.append('a');
        }
        Assert.assertNotNull(McpApplyChangesService.linkUrlError(url.toString()));
    }
}
