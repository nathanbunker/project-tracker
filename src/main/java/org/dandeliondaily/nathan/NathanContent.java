package org.dandeliondaily.nathan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class NathanContent {

    private static final String RESOURCE_PATH = "nathan/nathan_page_content.md";
    private static final String PUBLIC_MARKER = "# PUBLIC CONTENT";
    private static final String EXTENDED_MARKER = "# EXTENDED CONTENT";
    private static final String CAREER_MARKER = "# Career lessons";
    private static final String RESUME_MARKER = "# Résumé";
    private static final String IMPLEMENTATION_MARKER = "# Implementation-boundary notes";
    private static final String SUMMARY_MARKER = "### Short summary";
    private static final String NARRATIVE_MARKER = "### Full narrative";

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().escapeHtml(true).build();

    private final String publicHtml;
    private final String extendedIntroductionHtml;
    private final String extendedConclusionHtml;
    private final List<CareerStory> careerStories;

    public NathanContent() {
        this(loadResource());
    }

    NathanContent(String markdown) {
        String publicMarkdown = between(markdown, PUBLIC_MARKER, EXTENDED_MARKER);
        String extendedMarkdown = between(markdown, EXTENDED_MARKER, IMPLEMENTATION_MARKER);
        String careerMarkdown = between(extendedMarkdown, CAREER_MARKER, RESUME_MARKER);

        publicHtml = render(publicMarkdown);
        extendedIntroductionHtml = render(before(extendedMarkdown, CAREER_MARKER));
        extendedConclusionHtml = render(from(extendedMarkdown, RESUME_MARKER));
        careerStories = Collections.unmodifiableList(parseStories(careerMarkdown));
    }

    public String getPublicHtml() {
        return publicHtml;
    }

    public String getExtendedIntroductionHtml() {
        return extendedIntroductionHtml;
    }

    public String getExtendedConclusionHtml() {
        return extendedConclusionHtml;
    }

    public List<CareerStory> getCareerStories() {
        return careerStories;
    }

    private static List<CareerStory> parseStories(String markdown) {
        List<CareerStory> stories = new ArrayList<CareerStory>();
        String[] keys = { "serve-client", "authority-without-power", "build-more-ideas" };
        String[] headings = {
                "## 1. Serve the Client. Build the Field.",
                "## 2. Earn Authority Without Power",
                "## 3. Build More Ideas. Own Them Less."
        };
        for (int index = 0; index < headings.length; index++) {
            int start = markdown.indexOf(headings[index]);
            if (start < 0) {
                throw new IllegalStateException("Missing Nathan career story: " + headings[index]);
            }
            int end = index + 1 < headings.length ? markdown.indexOf(headings[index + 1], start) : markdown.length();
            String storyMarkdown = markdown.substring(start + headings[index].length(), end);
            String summary = between(storyMarkdown, SUMMARY_MARKER, NARRATIVE_MARKER);
            String narrative = from(storyMarkdown, NARRATIVE_MARKER);
            stories.add(new CareerStory(keys[index], headings[index].substring(3), render(summary), render(narrative)));
        }
        return stories;
    }

    private static String render(String markdown) {
        return RENDERER.render(PARSER.parse(removeDisplayNotes(markdown).trim()));
    }

    private static String removeDisplayNotes(String markdown) {
        StringBuilder cleaned = new StringBuilder();
        String[] lines = markdown.replace("\r\n", "\n").split("\n", -1);
        for (String line : lines) {
            if (!line.trim().startsWith("> **Display note:**") && !"## Hero".equals(line.trim())) {
                cleaned.append(line).append('\n');
            }
        }
        return cleaned.toString();
    }

    private static String between(String value, String startMarker, String endMarker) {
        int start = value.indexOf(startMarker);
        int end = value.indexOf(endMarker, start + startMarker.length());
        if (start < 0 || end < 0) {
            throw new IllegalStateException("Nathan content marker missing: " + startMarker + " or " + endMarker);
        }
        return value.substring(start + startMarker.length(), end);
    }

    private static String before(String value, String marker) {
        int end = value.indexOf(marker);
        if (end < 0) {
            throw new IllegalStateException("Nathan content marker missing: " + marker);
        }
        return value.substring(0, end);
    }

    private static String from(String value, String marker) {
        int start = value.indexOf(marker);
        if (start < 0) {
            throw new IllegalStateException("Nathan content marker missing: " + marker);
        }
        return value.substring(start + marker.length());
    }

    private static String loadResource() {
        InputStream input = NathanContent.class.getClassLoader().getResourceAsStream(RESOURCE_PATH);
        if (input == null) {
            throw new IllegalStateException("Nathan content resource is unavailable: " + RESOURCE_PATH);
        }
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Nathan content", e);
        }
        return content.toString();
    }

    public static class CareerStory {
        private final String key;
        private final String title;
        private final String summaryHtml;
        private final String narrativeHtml;

        CareerStory(String key, String title, String summaryHtml, String narrativeHtml) {
            this.key = key;
            this.title = title;
            this.summaryHtml = summaryHtml;
            this.narrativeHtml = narrativeHtml;
        }

        public String getKey() {
            return key;
        }

        public String getTitle() {
            return title;
        }

        public String getSummaryHtml() {
            return summaryHtml;
        }

        public String getNarrativeHtml() {
            return narrativeHtml;
        }
    }
}