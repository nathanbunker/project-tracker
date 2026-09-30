# Dandelion Daily: AI planning integration assessment

Please inspect the Dandelion Daily codebase and propose a practical implementation plan for making its planning and project data available to an AI assistant. **This is a codebase assessment and design task. Do not implement changes yet.** Ground your recommendations in the existing models, routes, services, UI, reports, migrations, and authorization patterns. Cite relevant files and functions in your findings. Where the current code changes the best design, explain why.

## What Dandelion is for

Dandelion Daily is my adaptation of Getting Things Done. Projects provide context for actions. I plan tasks with times, work through them one at a time, record what I did, and review my commitments in Dandelion. I want to talk with Codex or another AI assistant about my plans and work, then have the assistant read and update Dandelion on my behalf. Dandelion should remain the source of truth.

The assistant should help me think through a project or action: ask useful questions, improve an action description or time estimate, break work into smaller actions, identify prerequisites, perform preparatory work where appropriate, and create a follow-up action for me to review its work. It should take account of my commitments in other projects and the time I actually have available.

## New planning concept: monthly and weekly outlooks

I want a durable place in Dandelion to record what I intend to accomplish over the **next month** and **next week**, across projects. This is a new concept, probably requiring a new database table or related tables. Please inspect the existing model before choosing the schema. The immediate priority is to store, retrieve, and edit these outlooks through the MCP endpoint. Later I may incorporate the next-week outlook into my weekly report; that report integration is not a prerequisite for the initial capability.

An outlook should be tied to its period, remain available for subsequent planning conversations, and be distinguishable from scheduled actions or meetings. Recommend how to represent its language, dates, history or revisions, and any useful links to projects without forcing every intention into a task. Explain what minimal Dandelion UI would let me see and edit these records directly.

The intended rhythm is a short conversation about the month, then next week, followed by decisions project by project about what to do when. I expect to revisit and update the weekly outlook every week.

## Project and action context

The assistant needs to read the project's existing name, description or boundary, current focus, intended outcome, success criteria, open actions, recent work, and notes. I have a process for reviewing project language; these fields have distinct purposes. All project language should also be editable through MCP **after my approval**, so the description and focus can change as a project evolves. Keep the existing project identity unless I explicitly choose to rename it.

I also want editable **AI thoughts on a project**: observations, questions, or ideas the assistant wants to retain for a later session. I should be able to see, edit, and delete these in Dandelion. Distinguish an AI observation from an established project fact or a decision I made, and advise whether existing notes can support this or a separate record is better.

For actions, expose enough read and write capability to improve and reorganize my work. This includes description and other language, scheduling or when an action happens, estimated duration, action classification or commitment (the existing concepts may include **meeting, action, might, will**), notes associated with the action, and relevant relationships to projects and other actions. Inspect the actual semantics of those categories rather than assuming they are values of a single field. Include creating, updating, rescheduling, splitting, and, where appropriate, removing or replacing actions. Preserve work history and actual time separately from estimates if the current model makes that distinction.

The assistant also needs a cross-project planning view for a specified date range: upcoming meetings, scheduled actions, other commitments, and enough information to assess available time. A separate AI assistant may eventually add meeting commitments through the same Dandelion capabilities.

## Conversational approval and write-back

I want to review changes **in the AI conversation**. The assistant may propose a substantial reworking of actions plus outlook and project-note updates. I will correct the proposal, approve it once, and expect all the approved changes to be saved. Do not design a second, time-consuming approval queue inside Dandelion.

Recommend a single operation that applies a coherent set of approved changes together, with validation, a clear result, and protection against overwriting data that changed since the assistant read it. Explain how to handle partial failure, retries, and an audit trail in a way that fits the existing code. Reads and drafts must not mutate data. The assistant should not silently treat its suggestions as my commitments.

## Integration shape

My preferred starting point is an **MCP endpoint hosted within the Dandelion Daily application**. Codex should call Dandelion directly; a separately deployed MCP service is not the goal. The endpoint can use Dandelion's existing application and data-access logic. If an internal API or service layer is needed, explain how to share it with the application and a future meeting assistant without duplicating business rules.

Propose a small, task-oriented MCP tool surface rather than exposing raw database CRUD. At minimum, consider tools to retrieve a project and its context, retrieve cross-project planning context for a date range, retrieve and set monthly and weekly outlooks, manage project AI thoughts, and apply an approved package of project and action changes. Recommend practical response sizes, identifiers, pagination or filtering, authentication, and permissions based on the codebase and intended client.

## Deliverable

Please return:

1. A concise map of the relevant existing code and what it already supports.
2. Gaps against the capabilities above, especially outlook storage and editability of every language field through MCP.
3. A proposed data model and migration approach, calling out what should reuse existing tables.
4. A proposed MCP tool contract and the application operations underneath it, including the single approved batch update.
5. A phased implementation plan, starting with a useful end-to-end slice that stores and retrieves outlooks and project/action context, then adds safe write-back. State which work is necessary for that slice and which can wait.
6. Important design questions that require my decision, with your recommended answer for each.

Do not assume my current review workflow, weekly report, or project-language importer needs to be replaced. Identify how the new capabilities can fit them. Do not implement until I review this assessment.
