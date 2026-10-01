# Dandelion MCP Feedback

A running list of problems found and ideas raised while using the Dandelion Daily MCP
server (`/api/v1/mcp`) from Claude Code. Add new entries at the top of each section with
the date found. When something is fixed, move it to **Resolved** with the fix date rather
than deleting it.

See `docs/Dandelion_Daily_AI_Integration_Assessment.md` for the design and
`docs/MCP-Codex-Setup.md` for client setup.

## Recommended Changes for the Next Build

**Agreed with Nathan:** 2026-09-30. This is the work list for the implementing agent.
Details are in the referenced entries below. After deployment, Claude will test each
item against the **Verify** line and move fixed entries to **Resolved**.

General rules for all new tools:
- return a JSON object, never a bare list, in `structuredContent` (P-1);
- any tool that writes narratives, cadence, or project fields requires Nathan's explicit
  approval in conversation before it's called, and the tool descriptions should say so;
- the UI and the MCP should call the same service, so behavior can't drift.

| # | Change | Ref | Verify |
| --- | --- | --- | --- |
| 1 | Fix list results in `structuredContent` (wrap lists in an object) | P-1 | `get_day_availability` and `list_outlooks` return data instead of a schema error |
| 2 | Add `get_work_day_review(date)` | I-10 | Returns today's review list with minutes, reviewed flag and reason, completed and deleted actions, and existing narratives with ids, matching the review screen |
| 3 | Add `save_work_day_review(date, projectId, entries)`, using a service shared with `ProjectNarrativeReviewServlet` | I-10 | Saving through the MCP marks the project reviewed in the UI; omitted fields stay unchanged; an empty field removes that day's entry |
| 4 | Fix clearing a field in the review screen | P-7 | Clearing a Decision in the UI and saving removes it |
| 5 | Add narrative ids (and `lastUpdated`) to `get_project_context` | I-10, I-5 | `recentNarratives` entries include ids |
| 6 | Add narrative create, update, and delete for any date | I-5 | Add, edit, and remove a test narrative on an older date |
| 7 | Report daily report (`TrackerNarrative`) status in the work day review | I-10 | Review result shows whether the day's report was generated or approved |
| 8 | Add read-only access to daily and weekly narratives | I-8 | Last week's narrative can be read, with its review status |
| 9 | Add a tool to change a project's review cadence (`ProjectContactAssigned.updateDue`) | I-7 | Move a test project from Week to Month and see it on the Project Health page |
| 10 | Return the new `asOf` from `apply_changes` results | P-4 | An update followed by a reschedule works without a fresh read |

Not in this build (still open below): I-1 project list and health, I-2 waiting items,
I-3 day load summary, I-4 funding drift, I-9 attention modes (separate design, see
`docs/action-attention-modes.md`), P-2, P-3, P-5, P-6.

## Problems

### P-7: Clearing a review field doesn't remove the narrative

**Found:** 2026-09-30 (in the app, from reading the code; not tested)

In `ProjectNarrativeReviewServlet.saveNarratives`, Decisions, Insights, Risks, and
Opportunities are only upserted when the text is non-empty. If Nathan clears a field
that already has a narrative for that day and saves, the old narrative stays. The same
semantics should apply in the UI and in the proposed MCP tool (I-10): an emptied field
removes that day's entry.

### P-1: Tools that return a list fail MCP schema validation

**Found:** 2026-09-30
**Affects:** `get_day_availability`, `list_outlooks` (probably any tool whose result is a list)

The client rejects the whole result with:

```text
MCP server "dandelion" returned a malformed result that failed schema validation:
Invalid result for tools/call: expected record, received array (path: structuredContent)
```

**Cause:** `McpResource.toolSuccessResult()` sets
`result.set("structuredContent", mapper.valueToTree(value))`. When `value` is a `List`,
`structuredContent` becomes a JSON array. The MCP spec requires `structuredContent` to be
a JSON object, so the client discards the result, including the text block that would
have worked.

**Fix options:**
- Wrap list results in an object, e.g. `{"days": [...]}` or `{"outlooks": [...]}`.
  This is the most useful for clients.
- Or only set `structuredContent` when the node is an object and rely on the text block
  otherwise.

**Impact:** Planning can't check real availability. The assistant falls back to the
documented 8-hour weekday default, which may not match (see P-5). Outlook history can't
be reviewed.

### P-2: `get_planning_context` gets too large to use over a week

**Found:** 2026-09-30

An 8-day range (2026-10-02 to 2026-10-09) returned about 77,000 characters, which exceeds
Claude Code's per-result limit. The client saved it to a file and the assistant had to
parse it with a script. Most of the volume is template-managed routine items (prayers,
brushing teeth, email clearing, etc.) repeated every day with full field sets.

**Ideas:**
- `excludeTemplateManaged` or `workOnly` flag (for example, only actions whose project has
  a bill code).
- A compact mode returning only id, project, description, type, estimate, and dates.
- A per-day summary block (minutes by meetings, routine, tasks, and might), so load
  questions don't need the full list. See I-3.

### P-3: Some action descriptions look truncated

**Found:** 2026-09-30

Several descriptions end mid-phrase, as if a linked name or mention was dropped during
serialization:

| actionNextId | Description as returned |
| --- | --- |
| 796154 | "setup meeting agenda for" (since renamed) |
| 796000 | "setup project on" |
| 795750 | "brainstorm new name for this concept on" |
| 795969 | "review" (since removed) |
| 796162 | "with AIRA Co-Ag call" (meeting phrasing, probably fine) |

**To check:** whether these contain a link/mention token in the database or UI that the
MCP output strips. If so, include the linked name (and URL) in the output.

### P-4: `apply_changes` results don't return the new `asOf`

**Found:** 2026-09-30

After an `update_action`, the action's `asOf` changes, but the result only returns
`status: applied`. A follow-up change to the same action needs a fresh read first (this
happened with action 796106: update, then reschedule). `create_action` returns
`createdActionNextId` but also no `asOf`.

**Idea:** Include the new `asOf` (and ideally the updated action) in each result entry.

### P-5: Daily capacity default is unclear

**Found:** 2026-09-30

`get_day_availability` documents 8 hours on weekdays by default, but
`get_time_allocation_context` reports `obligatedMinutes: 2250` for the week (37.5 hours,
or 7.5 hours a day). With P-1 blocking availability reads, the assistant can't tell which
is right for a given day.

### P-6: Duplicate action created at the same timestamp

**Found:** 2026-09-30 (probably a Dandelion app issue rather than the MCP)

Actions 796171 and 796172 (Dandelion Daily, "continue with Phase 3", MIGHT, 20 min,
scheduled 2026-10-01) both had `asOf` 2026-09-30T20:45:24Z. Only 796171 had notes
(a `claude --resume` line). Nathan didn't create a duplicate intentionally. Possible
double submit in the create or "add note" flow. 796172 was removed through the MCP.

## Ideas

### I-10: Work day review through the MCP

**Raised:** 2026-09-30 (Nathan)

Let an agent run the end-of-day work day review (`ProjectNarrativeReviewServlet`): look
at the day, get project context, ask clarifying questions, and fill in the Notes,
Decisions, Insights, Risks, and Opportunities for each project, with Nathan's approval.
These narratives feed the daily report and the project's running history.

How the review works today (from the code):
- The review list is the projects with at least `MINUTES_REVIEW_THRESHOLD` billed
  minutes (`BillEntry`) on the date, sorted by minutes
  (`ProjectNarrativeDao.listReviewItemsForDate`).
- A project counts as reviewed when it has any narrative on that date **and** its setup
  is complete: `ProjectContactAssigned.updateDue > 0`, outcome, and success criteria.
- Each project shows completed actions (`ActionTaken` on the date) and deleted actions
  that had time.
- Saving upserts one narrative per verb per day. NOTE is always written (default
  "Reviewed/no comments"), which is what marks the project reviewed. Narrative times are
  the start of the day plus a few seconds, to keep the order.

Recommended tools:

1. **`get_work_day_review(date)`** (read). Mirrors the review screen for a whole day:
   - total minutes for the day, and the threshold applied;
   - per project, in minutes order: id, name, minutes, reviewed flag, and **why** it
     isn't reviewed yet (no narrative, or setup incomplete with the missing pieces named);
   - completed actions: description, completion note, completion time, and minutes when
     the time entry is linked to the action;
   - deleted actions with time;
   - the existing narratives for that date by verb, each with its id and `lastUpdated`;
   - optionally, projects below the threshold (`includeBelowThreshold`).
   Return an object (see P-1).

2. **`save_work_day_review(date, projectId, entries)`** (write, after approval). Same
   semantics as the review screen, so the UI and the MCP behave identically:
   - `entries` has optional `note`, `decision`, `insight`, `risk`, `opportunity`;
   - an omitted field is left unchanged; an empty string removes that verb's entry for
     the day;
   - NOTE gets the default text when nothing else marks the review;
   - use each existing narrative's `lastUpdated` as a staleness check.
   Best done by moving the save logic out of the servlet into a shared service that both
   the servlet and the MCP call.

3. **Narrative ids in `get_project_context`.** Recent narratives currently come back
   with date, verb, and text but no id, so the agent can't edit or delete them (I-5).

4. **Daily report status.** The daily report (`TrackerNarrative`) is built from these
   narratives. The review tool should say whether the day's report was already generated
   or approved, so the agent can tell Nathan when an edit means the report needs
   regenerating.

5. **Setup gaps.** When a project can't be marked reviewed because its setup is
   incomplete, the agent can fix the outcome and success criteria with
   `update_project_language`, and the review cadence with I-7.

How an agent would run it: read the day's review, then for each project (largest first)
read its context, ask Nathan one to three questions, draft only the fields that matter,
get approval, and save. Projects with nothing interesting get the default note.

Relationship to I-5: this is the day-scoped version of narrative editing. I-5 is still
needed for correcting or removing older narratives. Both should share one service.

### I-9: Action attention modes (Focus, Concurrent, Listen)

**Raised:** 2026-09-30 (Nathan)

A model change beyond the MCP, written up in `docs/action-attention-modes.md`. Once it
exists, the MCP should expose each action's mode, report load by mode, and let the
assistant suggest refilling the concurrent and listening queues. Supersedes I-6.

### I-8: Read daily and weekly narratives

**Raised:** 2026-09-30 (agreed with Nathan)

Expose the generated period narratives (`TrackerNarrative`) read-only, for example
`get_narratives(periodType, fromDate, toDate)`. They're high-level and span projects and
tasks, which gives context the per-project tools don't: what actually happened across
the week, recurring themes, and where attention went compared with the plan. Most useful
when reviewing last week before setting the next week's outlook.

Suggestions:
- return `markdownFinal` when present, otherwise `markdownGenerated`, with
  `reviewStatus` so the assistant knows whether Nathan approved it;
- include `periodStart`, `periodEnd`, `narrativeType`, and `displayTitle`;
- return an object, not a bare list (see P-1).

### I-7: Change a project's review cadence

**Raised:** 2026-09-30 (Nathan)

Let the assistant move a project between review cadences (the `ReviewInterval` values:
Week, Two Weeks, Month, Two Months, Four Months, Year, or none; stored per person as
`ProjectContactAssigned.updateDue`), for example moving a
project from weekly to monthly checking as priorities shift. This is the same grouping
the Project Health page uses. Pairs with I-1: read cadence and health, then propose
cadence changes for approval. Should require explicit approval, like
`update_project_language`.

### I-1: List projects with health, priority, and week focus

**Raised:** 2026-09-30 (Nathan)

There's no way to discover projects. The assistant only knows project IDs that appear in
planning results. A `list_projects` tool should return, per project:

- id, name, handle, status, bill code and funding source;
- priority level and review cadence (`ReviewInterval`, as grouped on the Project Health
  page);
- health: last activity date, whether it's overdue for attention against its cadence,
  open action count, overdue actions, and waiting items;
- whether it's a current **week** (top-priority) project.

This would let the assistant help Nathan focus on the projects that matter most, notice
neglected ones, and challenge whether the week projects are still the right ones.

### I-2: Surface waiting items and what they gate

**Raised:** 2026-09-30

Nathan uses WAITING actions that hide follow-on work until they're done (for example,
InteropHub Communication Bundles: the next phases appear only after the first phase is
deployed and tested). The assistant can't see that anything is queued behind a waiting
item, so it can wrongly conclude a project has no planned work. Idea: include waiting
items with a short "unblocks" summary in `get_project_context` and in I-1 health.

### I-3: Day load summary tool

**Raised:** 2026-09-30

Most planning questions are "am I overbooked?" A tool returning, per day: available
minutes, meetings, routine (template) minutes, committed tasks, might tasks, and overdue
items would answer that directly and avoid P-2.

### I-4: Funding-source drift watch

**Raised:** 2026-09-30

The time report already has targets and actuals by bill code. Useful additions:
- flag bill codes drifting from target over the 4-week window;
- note which gaps Nathan controls and which are externally driven (for example, CDC
  Technical Assistance depends on incoming requests, so Nathan can't raise it alone);
- note when targets are under review (percentages may change pending discussion with
  Nathan's manager).

### I-5: Create, edit, and delete project narratives through the MCP

**Raised:** 2026-09-30
**Priority:** Critical (Nathan)

This means project narratives (`ProjectNarrative`: dated entries with a verb), not the
generated daily/weekly narratives (`TrackerNarrative`, see I-8). `get_project_context`
already returns recent narratives, but there's no way to write them.

Needed:
- **add** a narrative (verb: NOTE, DECISION, INSIGHT, RISK, OPPORTUNITY, etc.; date; text);
- **update** a narrative's text, verb, or date;
- **delete** a narrative.

Editing and deleting matter as much as adding: things change constantly, and outdated
narratives regularly need to be corrected or removed so planning isn't fed stale
information.

Why it matters: narratives are included in planning, so decisions and insights made in a
planning conversation (like the Conformance Reboot decisions on 2026-09-30) belong there
rather than in AI thoughts, which are the assistant's own notes. Nathan built narratives
but hasn't used them much yet; MCP access would make them part of the normal workflow.

Writes should require explicit approval of the wording, like `update_project_language`.
Consider returning the narrative id from add, and using `lastUpdated` as a staleness
check on update and delete.

### I-6: Mark dev tasks as agent-runnable

**Raised:** 2026-09-30

Nathan keeps specced dev tasks (for example, InteropHub export, Dandelion Phase 3) as
MIGHT and runs them with an agent during meetings. A flag or tag for "agent-runnable
during meetings" would let the assistant leave them out of load totals, or suggest them
for meeting-heavy days, instead of treating them as overflow.

## Verified Working

**2026-09-30:**
- `get_planning_context` (single day, and filtered by project)
- `get_project_context`
- `get_time_allocation_context`
- `get_outlook` (single week and month; both empty)
- `update_project_language` (project 48903, with `lastModifiedAt` null, so no `asOf`
  needed; result returned the new `lastModifiedAt`)
- `add_project_ai_thought`: returned noteIds 1 and 2. This is the first live test of the
  AI-thoughts feature.
- `apply_changes`: `reschedule_action`, `update_action` (description, estimate, type,
  addNote), `remove_action` and `create_action`, including a 10-change batch

Not yet exercised: `set_outlook`, `set_day_availability`, `update_project_ai_thought`,
`delete_project_ai_thought`, `split_action`, `complete_action`.

## Resolved

None yet.
