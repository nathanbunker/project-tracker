package org.openimmunizationsoftware.pt.manager;

import java.util.Map;

import org.openimmunizationsoftware.pt.model.ProjectActionNext;
import org.openimmunizationsoftware.pt.model.ProjectActionTaken;
import org.openimmunizationsoftware.pt.model.ProjectNarrative;
import org.openimmunizationsoftware.pt.model.ProjectNarrativeVerb;

public class StubNarrativeGenerator implements NarrativeGenerator {

    @Override
    public String generateDailyMarkdown(GenerationContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Summary\n");
        sb.append("- Period: ").append(ctx.getPeriodStart()).append(" to ")
                .append(ctx.getPeriodEnd()).append("\n");
        sb.append("- Completed actions: ").append(ctx.getCompletedActions().size()).append("\n");
        sb.append("- Projects with time: ").append(ctx.getTimeByProject().size()).append("\n\n");

        sb.append("# Time By Project\n");
        if (ctx.getTimeByProject().isEmpty()) {
            sb.append("- No time tracked.\n\n");
        } else {
            for (Map.Entry<Integer, Integer> entry : ctx.getTimeByProject().entrySet()) {
                String name = ctx.getProjectNames().get(entry.getKey());
                sb.append("- ").append(name == null ? "Project " + entry.getKey() : name)
                        .append(": ").append(TimeTracker.formatTime(entry.getValue())).append("\n");
            }
            sb.append("\n");
        }

        sb.append("# Completed Actions\n");
        if (ctx.getCompletedActions().isEmpty()) {
            sb.append("- No completed actions recorded.\n\n");
        } else {
            for (ProjectActionTaken action : ctx.getCompletedActions()) {
                String projectName = action.getProject() == null ? "" : action.getProject().getProjectName();
                sb.append("- ");
                if (!isEmpty(projectName)) {
                    sb.append(projectName).append(": ");
                }
                sb.append(action.getActionDescription()).append("\n");
            }
            sb.append("\n");
        }

        appendNarrativeSection(sb, "Notes", ProjectNarrativeVerb.NOTE, ctx);
        appendNarrativeSection(sb, "Decisions", ProjectNarrativeVerb.DECISION, ctx);
        appendNarrativeSection(sb, "Insights", ProjectNarrativeVerb.INSIGHT, ctx);
        appendNarrativeSection(sb, "Risks", ProjectNarrativeVerb.RISK, ctx);
        appendNarrativeSection(sb, "Opportunities", ProjectNarrativeVerb.OPPORTUNITY, ctx);

        sb.append("# Waiting / Blocked\n");
        if (ctx.getWaitingActions().isEmpty()) {
            sb.append("- No waiting items recorded.\n");
        } else {
            for (ProjectActionNext action : ctx.getWaitingActions()) {
                String projectName = action.getProject() == null ? "" : action.getProject().getProjectName();
                sb.append("- ");
                if (!isEmpty(projectName)) {
                    sb.append(projectName).append(": ");
                }
                sb.append(action.getNextDescription()).append("\n");
            }
        }

        return sb.toString();
    }

    private static void appendNarrativeSection(StringBuilder sb, String title, ProjectNarrativeVerb verb,
            GenerationContext ctx) {
        sb.append("# ").append(title).append("\n");
        boolean added = false;
        for (ProjectNarrative narrative : ctx.getProjectNarratives()) {
            if (narrative.getNarrativeVerb() != verb) {
                continue;
            }
            String projectName = narrative.getProject() == null ? "" : narrative.getProject().getProjectName();
            sb.append("- ");
            if (!isEmpty(projectName)) {
                sb.append(projectName).append(": ");
            }
            sb.append(narrative.getNarrativeText()).append("\n");
            added = true;
        }
        if (!added) {
            sb.append("- None recorded.\n");
        }
        sb.append("\n");
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
