# Linked Meeting — Domain Model Proposal

## Purpose

A linked meeting represents one externally managed calendar occurrence associated with one Dandelion action.
It allows Dandelion to understand when a meeting occurs and retain the original meeting information without
requiring a person to enter or maintain the same information twice.

The model is intended to support synchronization through MCP first. It deliberately does not choose a final
calendar-integration architecture or define reminder and user-interface behavior.

---

## Core Principles

1. A meeting is a distinct domain concept, not merely a collection of fields on an action.
2. A meeting is always attached to exactly one action.
3. The relationship is optional: most actions do not have a meeting.
4. An attached action must have the `WILL_MEET` action type.
5. One meeting never spans multiple actions or projects.
6. Dandelion stores a read-only replica of externally owned meeting data.
7. Meeting synchronization is programmatic. The meeting is not manually created or edited through the UI.
8. The action remains the Dandelion-owned planning and work-tracking record.
9. Each occurrence of a recurring calendar series is represented independently.

This preserves Dandelion's existing action model while adding enough source identity and timing information for
reliable synchronization.

---

## Ownership Boundary

### Externally owned meeting data

The synchronization source controls:

- source identity
- occurrence identity
- original subject
- original description/body
- organizer, when available
- scheduled start and end
- source time zone
- location
- conferencing/join URL
- source status
- source revision metadata

Dandelion must not independently edit these values. They change only when a synchronization client writes a new
source snapshot through an authorized integration such as MCP.

### Dandelion-owned action data

Dandelion continues to control:

- project assignment
- focused action description
- estimated minutes
- notes selected for the user's purposes
- daily priority/order
- completion state
- actual time and completion description

The action description and notes do not need to reproduce the original invitation. The meeting preserves source
fidelity; the action can stay concise and useful for planning.

### Calendar duration versus action estimate

Meeting start and end represent the complete externally scheduled interval. The action's estimate represents the
time the user expects to spend.

These values may differ intentionally. For example, a 60-minute meeting can have a 30-minute action estimate when
the user plans to attend only part of it. The model does not record explicit planned join and leave times.

---

## Relationship to `ActionNext`

The relationship is one-to-one:

```text
ActionNext 1 ─── 0..1 Meeting
Meeting    1 ─── 1    ActionNext
```

Recommended invariants:

- `meeting.action_next_id` is non-null, unique, and references `action_next`.
- An action with a meeting must have `next_action_type = WILL_MEET`.
- A meeting cannot be reassigned to a second action implicitly.
- Linking an already-linked action or source occurrence must fail unless the request explicitly performs a safe
  reassignment.
- Completing or cancelling the action does not delete its meeting snapshot.

The model intentionally does not allocate one meeting across projects. The single attached action provides the
meeting's project holder even when its discussion crosses several topics.

---

## Occurrence-Level Identity

Recurring series must be materialized as individual meeting occurrences. A moved, cancelled, or otherwise modified
occurrence can then synchronize without changing the other occurrences in the series.

The durable source key should contain:

- source system
- source calendar/account identifier when needed to establish a namespace
- external event UID
- external occurrence identifier

For ICS input, the occurrence identifier should use `RECURRENCE-ID` when present. A stable canonical occurrence key
must also be produced for ordinary and non-exception instances, normally from the occurrence's original scheduled
start. The current displayed title and join URL are not identifiers: both can change, and a recurring series may reuse
the same URL.

Recommended uniqueness:

```text
UNIQUE(source_system, source_namespace, external_event_uid, external_occurrence_id)
```

The precise representation of `external_occurrence_id` may vary by source, but its stored value must remain stable
when an occurrence is moved to a different time.

---

## Proposed Meeting Fields

Names and database types are illustrative and should be aligned with existing project conventions during
implementation.

| Field | Required | Meaning |
|---|---:|---|
| `meeting_id` | Yes | Dandelion identity for the synchronized occurrence |
| `action_next_id` | Yes | Unique foreign key to the attached `WILL_MEET` action |
| `source_system` | Yes | Source type, such as `OUTLOOK_ICS` or a future provider |
| `source_namespace` | Conditional | Calendar/account namespace needed to make source IDs unique |
| `external_event_uid` | Yes | Stable external event or series identifier |
| `external_occurrence_id` | Yes | Stable identity of this occurrence within the event/series |
| `original_subject` | Yes | Subject exactly as supplied by the source |
| `original_description` | No | Original organizer body, retained separately from action notes |
| `organizer_name` | No | Organizer display name when supplied |
| `organizer_address` | No | Organizer address when supplied and appropriate to retain |
| `scheduled_start` | Yes | Absolute start instant |
| `scheduled_end` | Yes | Absolute end instant; must be after the start |
| `source_time_zone` | No | Original source time-zone identifier for fidelity and display |
| `location` | No | Source-controlled physical or virtual location text |
| `join_url` | No | Primary direct conferencing URL selected from the source |
| `source_status` | Yes | `CONFIRMED`, `TENTATIVE`, `CANCELLED`, or `MISSING` |
| `source_last_modified_at` | No | Source-provided modification time when available |
| `source_revision` | No | Source revision/sequence value when available |
| `source_fingerprint` | No | Hash or comparable value used to detect changed source content |
| `last_synced_at` | Yes | When Dandelion last accepted the source snapshot |

The system should store time instants unambiguously while retaining the original time-zone identifier separately.
Calendar all-day notices and colleague OOO conventions are filtering concerns for the synchronization process; they
do not require a linked meeting record when no Dandelion action should be created.

---

## Status and Lifecycle

### Confirmed and tentative

`CONFIRMED` and `TENTATIVE` reproduce the source state. Synchronization may update either state without altering the
attached action automatically unless an approved synchronization policy says otherwise.

### Cancelled

A cancelled occurrence remains visible as `CANCELLED`. Synchronization must not delete the meeting or automatically
close its action merely because the source cancelled it.

Keeping the action open allows the user to review whether follow-up work, communication, or replacement planning is
needed. A later tool or workflow may propose closing or changing the action, but that is separate from reproducing the
source cancellation.

### Missing

An occurrence that was previously synchronized but is no longer present in a source read should not be deleted
immediately. It should be marked `MISSING`, preserving evidence of the prior link and allowing uncertainty caused by
feed windows, publication problems, or temporary integration failures to be resolved.

The future synchronization design must define how many successful source reads, or what elapsed time, is required
before treating absence as `MISSING`.

### Historical retention

Completing, cancelling, or otherwise closing the action retains the attached meeting snapshot. This supports review,
auditability, and historical time analysis. Explicit unlinking or deletion should be a separate, deliberate operation.

---

## Join URL and Description Semantics

`meeting.join_url` is the source-controlled primary link used to enter the meeting. Synchronization should prefer a
direct Teams, Zoom, or equivalent join link and exclude help, meeting-options, dial-in-management, invitation-management,
or unrelated document links.

The existing action `linkUrl` can be mirrored during an incremental implementation so current Dandelion behavior can
launch the meeting. Long term, the linked meeting should be the authoritative source for the meeting join URL; the
application should avoid two independently editable copies.

`meeting.original_description` retains the source invitation body for fidelity. It may contain conferencing boilerplate,
signatures, or warnings. The action's notes contain only concise context useful to the user, such as an agenda, preparation
request, meeting purpose, or working-document link. Synchronization must avoid repeatedly appending the same derived note.

---

## MCP Requirements

MCP is the initial supported synchronization boundary. The eventual mechanism that reads calendars and invokes MCP
remains intentionally open.

### Read support

Action and planning reads should return an optional `linkedMeeting` object containing the complete meeting model,
including source identity, times, status, original content, join URL, and synchronization metadata.

Reads should support finding meetings by:

- attached action ID
- source occurrence key
- scheduled date or time range
- synchronization status

### Write support

MCP should support idempotent operations to:

- link or upsert a meeting occurrence on one `WILL_MEET` action
- synchronize changed source fields
- mark an occurrence cancelled
- mark an occurrence missing
- explicitly unlink a meeting when approved

Writes should use optimistic concurrency and support atomic batches with related action changes. An upsert using the
same source occurrence key must update the existing meeting rather than create a duplicate.

The MCP contract should clearly separate:

- the read-only source snapshot being synchronized
- optional proposed changes to Dandelion-owned action fields

Synchronizing a meeting must not silently overwrite the action's project, estimate, focused description, notes,
priority/order, completion state, or recorded time.

### Validation

MCP writes must reject:

- a meeting attached to a non-`WILL_MEET` action
- a second meeting attached to an already-linked action without an explicit resolution
- a source occurrence key already attached to another action
- an end at or before the start
- an invalid join URL
- source fields changed through a general action-edit operation

---

## Synchronization Responsibilities

The external synchronization process is responsible for:

1. Reading and resolving source occurrences, including recurrence exceptions and time zones.
2. Filtering entries that should not become meetings, such as colleague all-day OOO notices.
3. Matching a source occurrence to an existing action or proposing a new action.
4. Detecting schedule overlaps and presenting conflicts for review rather than deciding attendance automatically.
5. Synchronizing the complete meeting snapshot through MCP.
6. Proposing focused action fields separately when useful.
7. Ordering daily `WILL_MEET` actions by start time when approved.
8. Verifying the stored meeting and action state after a write.

The first implementation may continue to use clear title, date, duration, and project context for initial matching.
After a meeting is linked, subsequent synchronization should use the durable source occurrence key.

---

## Explicitly Out of Scope

This proposal does not define:

- a user interface for creating or editing linked meetings
- a final calendar connector or polling architecture
- Microsoft Graph, OAuth, webhook, or ICS-feed implementation details
- reminder delivery or notification channels
- automatic launching or forced switching to a meeting
- automatic conflict resolution
- explicit planned join and leave times
- allocating one meeting across multiple actions or projects
- writing changes back to the source calendar

The model may enable reminders, upcoming-meeting indicators, a Join action, conflict assistance, and work-transition
prompts later. Those capabilities should be designed separately after synchronization behavior has been exercised
through MCP.

---

## Open Implementation Decisions

The following should be resolved during technical design:

- concrete table, class, and field names
- time storage types and mapping conventions
- supported `source_system` vocabulary
- how source namespaces are identified without exposing credentials
- canonical occurrence identifiers for providers that do not supply `RECURRENCE-ID`
- rules and thresholds for marking an occurrence `MISSING`
- retention and explicit unlink/delete authorization
- treatment of very large or formatted original descriptions
- whether organizer addresses require additional privacy controls
- incremental compatibility behavior for the existing action `linkUrl`
- whether source updates can optionally propose, but never silently make, action-field changes

---

## Summary

A linked meeting is an occurrence-level, read-only replica of an external calendar meeting attached one-to-one to a
`WILL_MEET` action. It preserves the original meeting identity, description, schedule, status, and join information,
while the action remains focused on planning, project ownership, expected effort, completion, and time tracking.

MCP provides the first synchronization interface. Calendar integration, UI behavior, and reminders remain future
design concerns enabled by this model rather than requirements of it.
