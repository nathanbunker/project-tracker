package org.dandeliondaily.weeklyreport.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class SafeMarkdownRenderer {
    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().escapeHtml(true).build();

    public String render(String markdown) {
        if (markdown == null || markdown.trim().length() == 0) {
            return "";
        }
        Node document = PARSER.parse(markdown);
        return RENDERER.render(document);
    }
}