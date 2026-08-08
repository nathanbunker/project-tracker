package org.dandeliondaily.weeklyreport.service;

import org.junit.Assert;
import org.junit.Test;

public class SafeMarkdownRendererTest {
    @Test
    public void escapesEmbeddedHtmlButRendersMarkdown() {
        String rendered = new SafeMarkdownRenderer().render("# Summary\n<script>alert('x')</script>\n**Done**");
        Assert.assertTrue(rendered.contains("<h1>Summary</h1>"));
        Assert.assertTrue(rendered.contains("&lt;script&gt;"));
        Assert.assertFalse(rendered.contains("<script>"));
        Assert.assertTrue(rendered.contains("<strong>Done</strong>"));
    }
}