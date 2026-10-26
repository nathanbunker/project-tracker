# Topic Folders and Linked Topics in MCP — Proposal

## Purpose

Give each shared topic a single working folder that people and agents can find from Dandelion, so an agent can
prepare outreach (or any topic work) from three sources at once:

- the topic folder: internal notes, contacts, documents, research, drafts
- Dandelion: the private project's actions and history, plus the shared topic's actions from everyone linked to it
- InteropHub: the public topic page and the meetings where the topic appeared

Building Bridges is the first use case: one private project linked to many country and organization topics
(France, Canada, PAHO, ...), each of which already has a folder in Dropbox.

---

## Current Model (as built)

**2026-10-26 correction:** Dandelion's v5.3 meeting agenda/note tables are empty persistence scaffolding, not an
implemented meeting-history feature. Meeting agendas and notes are maintained in InteropHub. Retire that
scaffolding as described in the [linked-meeting reconciliation](linked-meeting-domain-model.md#reconciliation-with-v53-meeting-work);
retain the populated project/contact synchronization fields.

| Layer | What it holds | Where |
|---|---|---|
| InteropHub topic | Public-facing topic, meeting agendas and notes | InteropHub |
| Shared topic project | Topic synced from InteropHub into a patch (shared) workspace | `project` with `external_project_id`, `external_managed = Y` |
| Private project | One person's work, e.g. Nathan's "Building Bridges" | `project` in a private workspace |
| Patch link | Private project → shared topics, 1-to-N | `project_patch_link` (`DIRECT_PROJECT`, `PATCH_TAG`, `PATCH_ALL`) |
| Shared action set | One action recorded in both the private and the shared project | `action_set` with type `SHARED` |
| Meeting topic | A topic discussed in an InteropHub meeting | InteropHub; not Dandelion's dormant `meeting_session_topic` table |

Students will get their own private accounts and link their own private projects to the same shared topics. The
shared topic then collects actions from everyone working on it, while each person's private project stays theirs.

---

## Gap

An agent working through MCP cannot currently follow the chain from private project to topic:

1. `get_project_context` on a private project does not return its patch links or the shared topics they resolve to.
2. Shared topic projects do not expose `externalProjectId` or an InteropHub URL, so there is no stable key to match a
   folder or a meeting topic.
3. An action shows `sharedWithProjects` only after it is already in a shared action set. There is no MCP way to
   create a private action that is also recorded on a chosen topic.
4. There is no MCP read of a topic's shared action history from all linked users. Meeting topic notes must be
   obtained separately from InteropHub.

In practice (10/15/2026): 11 follow-ups from the IVC meeting were created in Building Bridges without any link to
their topics (PATH, UMC, Canada, India, ...), because the linked topics were not visible.

---

## Proposal Part 1: Topic Folder Convention (no code)

### Location

Each topic space has one root folder in Dropbox. Building Bridges already has one:

```text
C:\Users\NathanBunker\AIRA Dropbox\Nathan Bunker\Emerging Standards\Building Bridges\
  Australia\  Canada\  Dubai\  England\  France\  Japan\  Mexico\  New Zealand\  Norway\  PAHO\  Philippines\  WHO\
```

Dandelion should not store local paths: they differ per person and machine, and students will have different Dropbox
roots. The root is per-user configuration (for Nathan, a line in `T:\AGENTS.md`). Agents find a topic's folder by
scanning the root for the `topic.md` whose ID matches, never by folder name, so renames and spelling differences
("England" vs "UK") do not break the link.

### `topic.md`

Each topic folder gets a small identity file. Existing documents stay where they are. (IDs and URLs in the examples
below are illustrative.)

```markdown
---
interopHubTopicId: 131
dandelionTopicProjectId: 48950
topicSpace: Building Bridges
name: France
status: Active
---

One-paragraph summary of where things stand and why we are engaged.

## Key contacts
- Name, organization, role, how we know them, last contact (date)

## Related
- InteropHub: https://informatics.immregistries.org/hub/...
```

The IDs are the identity. Everything else is convenience and can drift without breaking anything.

### Optional working files

Add these only when needed. Agents create them rather than requiring them up front:

```text
notes.md       running log, newest first, dated entries
research/      agent-written briefs, dated, each with sources
outreach/      drafts and what was actually sent
```

Folders without a `topic.md` (for example `UC Long Beach Résumés`) are ignored by the convention. A topic root may
also have `_topic-space.md` with the shared workspace or tag it corresponds to.

### What does not go in the folder

Topic folders are visible to colleagues. Candid personal notes ("slow to respond", "avoid raising X") belong in the
private project's narratives or a private notes area, not in the shared folder. Agents may read both but write only
shared-safe content into the topic folder.

---

## Proposal Part 2: MCP Read Support

### `get_project_context` on a private project

Add `linkedTopics`, resolved from `project_patch_link`:

```json
"linkedTopics": [
  {
    "linkType": "PATCH_TAG",
    "patchWorkspaceId": 6,
    "patchTag": "Building Bridges",
    "topics": [
      {
        "projectId": 48950,
        "projectName": "France",
        "projectStatus": "Active",
        "externalSource": "interophub",
        "externalProjectId": "131",
        "interopHubUrl": "https://informatics.immregistries.org/hub/es/topic/131"
      }
    ]
  }
]
```

A `PATCH_TAG` or `PATCH_ALL` link should list the topics it currently resolves to, since that set changes as
InteropHub adds topics.

### `get_project_context` on a shared topic project

Add identity and linkage:

- `externalSource`, `externalProjectId`, `interopHubUrl`
- `linkedPrivateProjects`: only the caller's own private projects that link here. Other users' private projects are
  not exposed.

### New: `get_topic_context(projectId)`

One read for "everything Dandelion knows about this topic":

- shared actions, open and recent completed, with who recorded each
- stable InteropHub topic identity/link so the agent can obtain meeting history and notes from InteropHub under
  its authorization rules; do not depend on local meeting-topic or note-line tables
- contacts assigned to the topic project
- the caller's private actions linked to the topic

This is the main input for agent preparation work.

---

## Proposal Part 3: MCP Write Support

Extend `create_action` in `apply_changes` with an optional `topicProjectIds` list. When present, Dandelion creates the
action in the private project and records it on each listed shared topic through a `SHARED` action set, as the UI
already does. Validation:

- each topic must be reachable through one of the private project's patch links
- the caller must be a member of the topic's patch workspace

Also useful: `update_action` with `addTopicProjectIds` to link an existing private action, so the 10/15 Building
Bridges follow-ups can be connected after the fact.

---

## Agent Workflow (outside Dandelion)

A workflow document, for example `T:\system\workflows\prep-outreach.md`, describes how an agent prepares to contact
a topic's organization:

1. Resolve the topic: private project → `linkedTopics` → topic project → `externalProjectId`.
2. Find the folder: scan the topic-space root for the matching `topic.md`.
3. Read the folder: `topic.md`, `notes.md`, existing documents (overviews, interview notes, memos).
4. Read Dandelion: `get_topic_context` for shared history, plus the private project's narratives.
5. Read InteropHub: the topic page and meetings that mention it.
6. Search the web for recent public information, citing sources.
7. Write a dated brief to `research/` and a draft to `outreach/`. Never send anything.
8. Propose Dandelion actions (for example "send intro email to ...") for approval.

---

## Rollout

| Phase | Work | Code? |
|---|---|---|
| 0 | Add `topic.md` to existing Building Bridges folders; write the workflow; point `T:\AGENTS.md` at the root. Link actions by putting the InteropHub topic URL in notes. | No |
| 1 | MCP reads: `linkedTopics`, topic identity fields, `get_topic_context` | Yes |
| 2 | MCP writes: `topicProjectIds` on create, `addTopicProjectIds` on update | Yes |
| 3 | Students: private accounts linked to the same topics; confirm Dropbox access to topic folders | Config |

Phase 0 can be piloted on one or two topics (Canada is a good test: it appears in both Building Bridges and the NUVA
scientific committee follow-up) before any MCP work.

---

## Open Questions

- Does every Building Bridges folder correspond to an InteropHub topic today, or do some topics need to be created
  (for example India, Brazil, Ireland, Switzerland)? Are organizations (PATH, UMC, STC Health) topics, or contacts
  under a country or a general topic?
- Should `get_topic_context` show other users' shared actions with their names, or anonymized for students?
- Should the topic folder root be recorded per patch workspace or tag (for example a shared Dropbox link on the
  topic space) so students can find it without local configuration?
- Which InteropHub read interface should agents use to retrieve a topic's meeting history and notes?
- Should Dandelion store a shareable Dropbox URL on the topic project (not a local path) as a convenience link?
