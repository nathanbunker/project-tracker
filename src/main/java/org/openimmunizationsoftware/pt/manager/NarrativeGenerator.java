package org.openimmunizationsoftware.pt.manager;

public interface NarrativeGenerator {
    String generateMarkdown(String narrativeType, GenerationContext ctx);
}
