package org.openimmunizationsoftware.pt.manager;

public interface NarrativeGenerator {
    String generateDailyMarkdown(GenerationContext ctx);
}
