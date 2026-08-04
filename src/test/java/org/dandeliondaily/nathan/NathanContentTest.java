package org.dandeliondaily.nathan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class NathanContentTest {

    @Test
    public void separatesPublicExtendedAndCareerStoryContent() throws Exception {
        String markdown = new String(Files.readAllBytes(Paths.get("src/main/resources/nathan/nathan_page_content.md")),
            StandardCharsets.UTF_8);
        NathanContent content = new NathanContent(markdown);

        assertTrue(content.getPublicHtml().contains("Nathan Bunker"));
        assertTrue(content.getPublicHtml().contains("Dandelion Daily started as a tool I needed myself"));
        assertFalse(content.getPublicHtml().contains("How I create value"));
        assertFalse(content.getPublicHtml().contains("Display note"));

        assertTrue(content.getExtendedIntroductionHtml().contains("How I create value"));
        assertFalse(content.getExtendedIntroductionHtml().contains("Vaccination is one of"));
        assertTrue(content.getExtendedConclusionHtml().contains("Professional experience"));

        assertEquals(3, content.getCareerStories().size());
        assertEquals("serve-client", content.getCareerStories().get(0).getKey());
        assertEquals("authority-without-power", content.getCareerStories().get(1).getKey());
        assertEquals("build-more-ideas", content.getCareerStories().get(2).getKey());
        assertTrue(content.getCareerStories().get(0).getSummaryHtml().contains("Indian Health Service"));
        assertTrue(content.getCareerStories().get(0).getNarrativeHtml().contains("Vaccination is one of"));
    }

    @Test
    public void hashesTokensWithSha256() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                NathanAccessService.hashToken("abc"));
        assertEquals(64, NathanAccessService.hashToken("another token").length());
    }
}