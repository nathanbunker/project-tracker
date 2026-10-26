# MCP Requests: Projects, Action Search, Related Projects

## Purpose

Four MCP additions requested on 2026-10-26 after syncing a week of Outlook meetings into Dandelion through the
MCP (`T:\system\workflows\outlook-calendar-to-dandelion.md`). They cover the gaps that remain once
[linked meetings](linked-meeting-domain-model.md) are deployed.

Linked meetings (5.12.0) already cover two problems found in that session:

- **Durable meeting identity.** Occurrence and series keys replace title matching after the first link, and
  series project suggestions show where earlier occurrences were filed.
- **Meeting times.** `scheduled_start` and `scheduled_end` on the linked meeting give each meeting a real time,
  so no general action start time is needed.

The remaining requests are:

1. [List projects](#1-list_projects)
2. [Search actions](#2-search_actions)
3. [Link an action to a related (shared) project](#3-related-projects-on-an-action)
4. [Create projects](#4-create-projects)

All writes follow the existing `apply_changes` rules: explicit user approval, `asOf` concurrency checks, and atomic
batches.

## Implementation contract (5.12.1)

The sections below this contract preserve the original request. The implemented scope is project discovery,
historical action search, private project creation, and guarded moves. **Related-project links (request 3) are
deferred**, not implemented: they require a visibility-only relationship and a separate privacy/schema design.
Existing SHARED action sets represent replicated actions with coupled edits/completion, not visibility-only links.
There is no new database/model migration for 5.12.1; the major/minor remains aligned with schema 5.12.

### Discovery and project context

`list_projects` accepts optional `nameContains`, `scope`, `status`, `billCode`, `tag`, `limit`, and `offset`.

- Scope: `ALL` (default), `PRIVATE`, or `SHARED`. Authorized workspaces are the API client's own workspace and
  the authenticated owner's active memberships in active shared (`PATCH`) workspaces. Another private workspace
  is never added through shared membership discovery.
- Status: `OPEN` (default, excluding `Complete` and `Closed`), `ALL`, or exact `Active`, `Paused`, `Complete`,
  `Closed`. Names are literal case-insensitive substrings; `%` and `_` are not wildcards. Bill code is exact and
  tag is an exact case-insensitive tag label.
- Pagination: `limit` 1-200 (default 100), nonnegative `offset` (default 0). Result:
  `{projects, hasMore, nextOffset}`. Follow `nextOffset` only when `hasMore`; ordering is name then project ID.
  As with other offset reads, concurrent changes can shift page boundaries.
- Each project includes identity/name, workspace ID/name, scope, `ownerWebUserId`, status/bill code,
  `externalSourceKey`, `externalProjectId`, `externalManaged`, `lastModifiedAt`, and `lastActivityAt`.
  Activity is the latest known project modification, action change, or recorded-work timestamp, not a fabricated
  creation/completion date. Empty projects are included; activity may be null.
- `get_project_context(projectId)` accepts these discovered shared project IDs. Cross-workspace context omits
  action sibling-project identities and linked-meeting source snapshots. Shared read permission does not expand
  any write tool's workspace scope or allow creating shared projects.

Examples:

```json
{"nameContains":"AIRA AI Development","scope":"ALL","status":"OPEN","limit":100,"offset":0}
```

```json
{"nameContains":"ImmDS + HALO","scope":"SHARED"}
```

### Historical action search

`search_actions` requires `text`, `startDate`, and `endDate`. Dates are inclusive `yyyy-MM-dd`, ordered, with at
most 366 days in a request. It searches the **client workspace only**; shared-project discovery does not widen
historical action search.

Optional filters:

- `includeNotes` (boolean, default false): searches persisted action notes as well as description.
- `actionTypes`: nonempty list using actual stored values: `WILL`, `WILL_CONTACT`, `WILL_MEET`, `REVIEW`,
  `DOCUMENT`, `WILL_FOLLOW_UP`, `MIGHT`, `WOULD_LIKE_TO`, `COMMITTED_TO`, `GOAL`, `WAITING`, `OVERDUE_TO`.
- `statuses`: nonempty list of `OPEN`, `COMPLETED`, `CANCELLED`; omitted means all three. Returned action status
  retains existing MCP codes (`P`, `R`, `C`, `X`).
- `projectIds`: nonempty list of up to 100 positive IDs; workspace authorization still applies.
- `hasLinkedMeeting`: boolean. False includes only unlinked actions.
- `limit` / `offset`: same bounds and continuation fields as project discovery; stable action-ID ordering.

Result: `{actions, hasMore, nextOffset}`. Actions include project identity, description, schedule, notes, `asOf`,
template flags, and linked-meeting snapshot. Private sibling-project identities are not returned by this search.

**History limitations:** the legacy work table references an action set, not an individual action. A set may contain
unrelated actions created in the same batch. Work is attributable only when exactly one action anywhere references
that set and its work matches the action's current project/workspace. This deliberately leaves ambiguous dates
unknown, including shared/batch siblings and moved actions whose work remains under the original project.

The range matches scheduled date **or** attributable recorded-work date for completed/cancelled actions.
`completionDate` is that recorded-work timestamp for a completed action (the existing completion service records
the start of the completed work interval), **not a precise finishing instant**. `recordedWorkDate` also applies to
cancelled actions with attributable work. Unknown completion dates are null with `completionDateKnown:false`.
An action modification timestamp is never substituted. Ambiguous historical actions remain searchable by their
scheduled date; widen the range rather than assuming that no result proves the action never existed.

```json
{
  "text":"Repository",
  "startDate":"2026-10-01",
  "endDate":"2026-10-31",
  "includeNotes":true,
  "statuses":["OPEN","COMPLETED","CANCELLED"],
  "limit":100
}
```

### Private project creation and safe moves

After explicit approval, `apply_changes` accepts:

| Change | Fields |
|---|---|
| `create_project` | Required `ref`, `name`, `billCode`; optional `description`, `currentFocus`, `tags`, `projectHandle`, `allowDuplicate` |
| `create_action` | Existing fields, with exactly one `projectId` or `projectRef` |
| `update_action` | Existing `actionNextId`, `asOf`, editable fields, plus optional `projectId` or `projectRef` to move |

References are unique within the batch and must refer to an **earlier** `create_project`. Never supply both
`projectId` and `projectRef`. Each create-project result returns its `ref` and persisted `projectId`. References
do not persist as external identities and are not usable in a later request.

Creation rules:

- Active private workspace membership is required. The owner is the authenticated user, status is `Active`,
  and `externalManaged` is false. An owner-contact assignment is created (`emailAlert:Y`, `updateDue:0`).
- Names are trimmed and limited to 100 characters; description is limited to 1,200 characters.
  Description/current focus/tags default to empty or absent.
- `billCode` must already exist in the client workspace. Tags must identify existing active workspace tags by
  name or handle (case-insensitive); this operation does not invent billing codes or tags.
- An open visible project with a matching name is rejected unless `allowDuplicate:true`; the batch is also checked
  for duplicate names and references. A duplicate-name override does **not** bypass handle uniqueness.
- The optional handle defaults from the name, trimmed and clipped to 60 characters, and follows existing UI
  validation (including rejecting hyphens and colons). Supply an explicit distinct `projectHandle` if needed.

Move rules:

- Only open, non-template actions can move. Template roots, generated instances, SHARED action-set members,
  closed actions/destinations, and cross-workspace moves are rejected.
- Source and destination must be in the same active private workspace. Today's current time entry is treated as
  a possible running timer under the existing tracker convention; stop/switch the timer before moving its action.
- An action cannot also be mutated by conflicting changes in that batch, including an `order_day` request that
  explicitly includes it. Reread the action and use its current `asOf`; a successful move returns a newer token.
- The action's ID, schedule, notes, estimates, action set, and linked meeting remain intact. Only planning ownership
  moves; future action billability is recalculated from the destination. Old/new project IDs are audited.
- Existing recorded time, work records, billing allocations, and narratives stay in their original projects.
  This can legitimately leave an old time entry pointing to an action now owned by another project. An existing
  entry's unchanged historical project/action pairing remains editable; arbitrary mismatched pairings and new
  mismatched entries remain invalid.
- Validation covers the entire batch before creation/movement begins. An invalid reference or other rejected
  change does not leave an empty new project behind.

Example: replace the billing code, action ID, and concurrency token with values from current reads.

```json
{
  "changes":[
    {"type":"create_project","ref":"repository","name":"Repository","billCode":"EXISTING_CODE"},
    {"type":"update_action","actionNextId":797163,"asOf":"2026-10-26T12:00:00Z","projectRef":"repository"},
    {"type":"create_action","projectRef":"repository","description":"First repository task",
     "nextActionType":"WILL","scheduledDate":"2026-10-26","estimateMinutes":15}
  ]
}
```

### Owner-run deployment and agent review

Local release validation: `mvn clean package` passed **419 tests across 65 suites**, with zero failures, errors,
or skips. This includes real MySQL tests in disposable schemas for visibility/history queries, shared context,
whole-batch rejection, create-project/move/create-action references, unchanged historical billing associations,
and linked-meeting preservation. JSON-RPC dispatcher tests cover tool advertisement and invocation. The packaged
WAR reports 5.12.1 and contains current-build classes. Disposable schemas were removed. No application database
migration, local Tomcat deployment, or production call was performed.

The release is built locally; deployment and production verification are intentionally left to Nathan.
Use a disposable project/action for write checks and explicitly approve the batch before calling it.

1. Deploy the clean-built 5.12.1 WAR and confirm its running footer version. Schema 5.12 must already be present;
   if deploying all linked-meeting work together, apply the documented 5.11 removal and 5.12 addition first.
2. Reconnect the agent and confirm `tools/list` advertises `list_projects`, `search_actions`, and the new
   `apply_changes` fields.
3. Find a known empty private project and a shared project by name, page results, and read both with
   `get_project_context`. Check external sync metadata; confirm unauthorized private projects remain invisible.
4. Search known open, completed, and cancelled actions by description and note text. Check literal `%`/`_`
   behavior, inclusive date boundaries, filters, and continuation. Treat unknown historical completion dates as
   unknown rather than assuming no match means no past action.
5. Create a disposable private project, move an existing open test action to it, and create a new action using
   the returned project reference in one batch. Confirm the existing action retains its ID, notes, schedule,
   estimates, action-set identity, and linked-meeting identity/version.
6. Confirm old recorded time and narratives stay in the original project. Confirm future work uses the
   destination project's billing settings. Check that an unchanged historical time-entry association remains
   editable after the move and cannot be forged for a new entry.
7. Try stale `asOf`, template-managed, closed-action, SHARED-set, running-timer, foreign-workspace, invalid-ref,
   duplicate-name, and invalid-bill-code cases. Confirm whole-batch rejection creates no project/action.
8. Record actual production-agent results in [MCP-Feedback.md](MCP-Feedback.md), including any failures.
   Do not mark the deployment verified based only on the local automated build.

---

## What went wrong without these

During the 2026-10-26 sync:

- No WILL_MEET actions existed for the week, so every meeting needed a project. The only way to find which project a
  recurring meeting belonged to was to read the previous week's time entries day by day.
- "AIRA AI Development" (48682) had no open actions and no fiscal-year time, so it didn't appear in any read. The
  assistant had to ask for its ID.
- A shared "ImmDS + HALO" project couldn't be confirmed or linked. The ImmDS + HALO meeting was filed under Emerging
  Standards Program with a note naming the shared project.
- The Repository Touch Point meeting needs a new project. The assistant couldn't create it, so it added a reminder
  action instead. It also can't move the meeting into the new project later, because `update_action` doesn't
  change `projectId`.

**Note on I-1.** [MCP-Feedback.md](MCP-Feedback.md) marks I-1 ("List projects with health, priority, and week
focus") as addressed on 2026-10-01, but the MCP has no tool that lists projects. `get_outlook` and `list_outlooks`
return free-text outlooks, and `get_project_context` needs an ID that's already known. Request 1 below reopens
the discovery part of I-1.

---

## 1. `list_projects`

A read-only tool that lists every project the user can see, both private and shared.

### Inputs (all optional)

| Input | Meaning |
|---|---|
| `nameContains` | Case-insensitive match on any part of the project name |
| `scope` | `PRIVATE`, `SHARED`, or `ALL` (default `ALL`) |
| `status` | Open or closed; default open |
| `billCode` | Only projects with this bill code |
| `tag` | Only projects with this tag |
| `limit` | Maximum number of results, with a continuation value for the next page |

### Output per project

- `projectId`, `projectName`
- `scope` (private or shared) and owner
- `externalSourceKey`, `externalProjectId`, and `externalManaged` when the project comes from sync (for example,
  InteropHub)
- status and bill code
- last activity date

### Why

This resolves names such as "AIRA AI Development" or "ImmDS + HALO" to an ID without guessing, before creating any
action. `externalManaged` tells the assistant which projects sync controls, so it doesn't propose local edits to
fields it can't change.

---

## 2. `search_actions`

A read-only tool that searches actions by text over a date range, **including completed and cancelled actions**.

### Inputs

| Input | Required | Meaning |
|---|---:|---|
| `text` | Yes | Case-insensitive match on the description |
| `includeNotes` | No | Also match notes |
| `startDate`, `endDate` | Yes | Inclusive range over scheduled or completion date; cap the span as other reads do |
| `actionTypes` | No | Same values as `get_planning_context` |
| `statuses` | No | Open, completed, cancelled |
| `projectIds` | No | Only these projects |
| `hasLinkedMeeting` | No | Only actions with (or without) a linked meeting |

### Output per action

- `actionNextId`, `projectId`, `projectName`
- description, action type, status
- scheduled date and completion date
- a linked-meeting summary when there is one (source status and scheduled start)

### Why

Series lookup on linked meetings covers meetings that are already linked. Plain search is still needed for:

- unlinked meetings created before linked meetings existed
- one-off meetings with no series history
- checking whether a to-do already exists before creating it (for example, "create Repository project")

Today `get_planning_context` returns only open actions, so history has to be rebuilt from time entries.

---

## 3. Related projects on an action

Let an action show up under one or more additional (often shared) projects without changing which project owns it.

### Changes in `apply_changes`

| Change type | Fields |
|---|---|
| `link_action_project` | `actionNextId`, `asOf`, `projectId` |
| `unlink_action_project` | `actionNextId`, `asOf`, `projectId` |

Action reads (`get_planning_context`, `get_project_context`, `search_actions`) return a `relatedProjects` list of
`{projectId, projectName}`.

### Rules

- The action's own project still owns the action, its recorded time, its estimate, and any linked meeting. This keeps
  the linked-meeting rule that one meeting never spans multiple projects.
- A related project only adds the action to that project's view, for example in `get_project_context` for the shared
  project.
- Reject linking the owning project, a project that is already linked, or a project the user can't see.

### Why

The ImmDS + HALO Working Session is Emerging Standards Program work, but it should also show up under the shared
ImmDS + HALO project. Today that link exists only as text in the action's notes.

---

## 4. Create projects

### Change in `apply_changes`

`create_project` with fields `{ref, name, description?, billCode, tags?, currentFocus?}`.

- `ref` is a placeholder name used only inside the batch. Later changes in the same batch refer to the new project
  with `projectRef` in place of `projectId`, the same way linked-meeting upserts can already link an action created
  earlier in the batch.
- The result returns the new `projectId` for each `ref`.

### Rules

- New projects are always private and local (`externalManaged = false`). Shared projects keep coming from the
  InteropHub sync API, so the two sources don't collide.
- Reject a name that matches an open project the user can see, unless the request explicitly allows a duplicate.
- Requires explicit approval, like every other `apply_changes` write.

### Also needed: move an action to another project

Add an optional `projectId` (or `projectRef`) to `update_action`. Rules:

- Moving an action also moves its linked meeting, because the meeting follows its action.
- Recorded time entries stay with the project they were billed to; only open planning moves.
- Reject moves on template-managed actions, as other template edits are rejected.

### Why

Turning Repository work into its own project becomes one approved batch:

1. Create the Repository project.
2. Move the Repository Touch Point meeting (action 797163) into it.
3. Add its first action.

The placeholder action created on 2026-10-26 (797164, "Create Repository project and move Repository Touch Point into
it", under AIRA AI Development) would then be unnecessary.

---

## Summary

| # | Request | Kind | 5.12.1 scope |
|---|---|---|---|
| 1 | `list_projects` (private and shared) | Read tool | Implemented; owner deployment review pending |
| 2 | `search_actions` (including completed) | Read tool | Implemented with explicit historical-date limitations |
| 3 | Related projects on an action | New relationship and write types | Deferred pending model/privacy design |
| 4 | `create_project` and moving actions between projects | `apply_changes` types | Implemented with private-workspace and move guards |
