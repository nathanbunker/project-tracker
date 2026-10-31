## Plan: action notes become a single editable notepad

Today an action's notes are a log of separate, dated lines (`action_next_note` rows). Nathan wants
notes to be **one plain-text value per action that he edits like a notepad**. The per-entry log
(dates and authors per line) is no longer wanted. This touches the model, every page that shows or
writes notes, and the MCP/API layer.

Decisions below were made with Nathan; treat them as settled:

- **Single value.** Notes are one text field per action. No per-entry dates, authors or history.
- **Stored as typed.** Keep blank lines, indentation and leading dashes exactly as entered.
- **No " - " prefixes.** The current read path prefixes every line with `" - "`; drop that
  everywhere, including MCP output. Downstream MCP clients will adapt.
- **No automatic date stamps** on appended text.
- **Quick append stays.** A one-line "add a note" stays as a convenience. It appends a line to the
  end of the text; it is not a separate entry.
- **Twins share notes.** A linked-project action is the same action recorded twice, once on the
  private project and once on the shared project (an `ActionSet` of type `SHARED`). Its twins must
  always hold the same notes text. See "Twins" below.

Make each phase deployable on its own. Bump `<version>` in `pom.xml` for each deployable build.

---

### How notes work today

**Storage.** `action_next_note` (created in `src/db/v5.1.sql`), with one row per note line:
`action_next_note_id`, `action_next_id`, `contact_id`, `note_line TEXT`, `note_date DATETIME`.
- Mapping: `ActionNextNote.java` / `ActionNextNote.hbm.xml`, listed in
  `src/main/resources/hibernate.cfg.xml`.
- `ActionNext.hbm.xml` maps it as the set `nextNoteEntries`
  (`cascade="all-delete-orphan"`, ordered by `note_date, action_next_note_id`).

v5.1 moved notes *from* an `action_next.next_notes` text column *into* these rows. This plan moves
them back to a text column, this time with one shared service, unchanged formatting and twin
syncing.

**`ActionNext` helpers.**
- `getNextNotes()` joins the rows by date, prefixing each line with `" - "` and skipping blank
  ones.
- `setNextNotes(String)` **clears every row** and recreates one row per non-blank line. All rows
  get `now` as the date and the action's `contactId` as the author, and a leading `-` is stripped.
- `setNextNotesIfChanged(String)` compares normalized text first.

So any full-text save already destroys the per-line dates.

**Writers. They disagree, and most ignore twins.**

| Where | What it does | Twins? |
|---|---|---|
| `DandelionDashboardServlet.applyEditFieldsToSiblings` (`nextNote` param, used by the shared Edit box via `editAction` / `CompleteActionEdit`) | `setNextNotesIfChanged`: full rewrite | yes |
| `DandelionDashboardServlet.handleAddCurrentActionNote` (`action=addCurrentActionNote`) | Reads `getNextNotes()`, appends `"\n - " + note`, then `setNextNotes`: full rewrite | **no** |
| `FocusedActionServlet.handleAddCurrentActionNote` (`action=addCurrentActionNote`, the Notes drawer) | Saves one new `ActionNextNote` per line | **no** |
| `mobile/servlet/ActionServlet` (two places, `PARAM_NEXT_NOTE`) | Saves new rows per line | **no** |
| `McpApplyChangesService.applyUpdate` (`update_action.addNote`) | Saves **one** row with the whole text, even when it has several lines | **no** |
| `McpApplyChangesService.applyCreate` (`create_action.notes`) | `setNextNotes` | n/a |
| `PlanAheadMutationService.saveTemplateEdit` (templates) | `setNextNotes` | n/a |
| `TemplateGenerationService` (around line 408) | Copies the template's notes into each generated instance | n/a |
| `DandelionDashboardServlet` around line 699 (adopting an AI-suggested next action) | `setNextNotes` | n/a |

**Readers.**
- `DandelionDashboardServlet.handleLoadActionData`: `nextNote` for the shared Edit box
  (`src/main/webapp/js/action-editor.js`, field `aeNote`).
- `FocusedActionServlet.extractNoteLines`: the Notes drawer and the context-strip preview.
- `DashboardNowColumnService.parseNotes`: Dashboard "now" column.
- `mobile/servlet/ActionServlet.extractNoteLines`.
- `McpActionContextSupport` (`map.put("notes", action.getNextNotes())`): every MCP action context.
- `McpDiscoveryService` around line 140: `search_actions` note search, an `exists` subquery on
  `ActionNextNote.noteLine`.
- `WeeklyReportDataService` around line 219.
- `TemplateManagementServlet` around line 415 (template notes textarea).

`extractNoteLines` and `splitNoteLines` are copied in `FocusedActionServlet` and the mobile
`ActionServlet`, and the date sort comparator is copied four times. All of these go away.

**Data, from the local copy of prod on 2026-10-31.**
- 394 rows on 287 actions, about 1.4 per action, at most 13.
- Only 9 actions have notes at more than one distinct `note_date`.
- No note's `contact_id` differs from its action's `contact_id`.
- 71 rows already contain newlines, mostly from MCP `addNote`.
- 1 template has notes.
- Longest line: 4,280 characters.
- Nothing in the UI shows a note's date or author.

---

### Target design

**Column.** Add `action_next.next_notes TEXT NULL`, holding the notes exactly as typed. Map it as a
plain `nextNotes` property on `ActionNext`. `getNextNotes()` / `setNextNotes()` become a simple
getter and setter with no prefixing, splitting or dash stripping. Remove `setNextNotesIfChanged`;
callers compare strings themselves, or the service does.

**One service: `ActionNotesService`** (for example in `org.dandeliondaily.shared.service`). Every
UI page **and** MCP calls it. Per the project rule in `CLAUDE.md`, the UI and MCP call the same
service. Nothing else writes `nextNotes`.

- `String read(ActionNext action)`
- `Result replace(Session, ActionNext action, String newText, String baseText)`: the notepad save.
  `baseText` is the text the editor started from; see Concurrency below.
- `void append(Session, ActionNext action, String text)`: quick add and MCP `addNote`.
  - If the current text is empty, the result is `text`.
  - Otherwise it is the current text plus a newline (only if it doesn't already end in one), then
    `text`.
  - Trim trailing whitespace from `text` only. Never add prefixes.
- Both write methods apply the new text to **every twin** (see Twins) and set `nextChangeDate` the
  way callers do now. MCP uses `McpActionContextSupport.truncatedNow()` and its
  monotonic-change-date rules, so keep MCP's own change-date handling.
- Normalize line endings to `\n`.
- Cap length at a generous limit, such as 20,000 characters. Reject longer text clearly rather than
  truncating it silently.

**Twins.** `DandelionDashboardServlet.resolveSharedActionSiblings(session, action)` returns the
action plus its `SHARED` action-set siblings. Move that lookup into the service, or into a small
shared helper both can use, so notes writes reach every sibling.

Template-generated instances are separate actions, not twins. Copying the template's notes when an
instance is generated stays as it is, now as plain text.

**Concurrency, so edits aren't lost.** Several writers can touch one text value (you in the
notepad, an MCP agent appending), so `replace` takes the text the editor started from:

- If the current text equals `baseText`, write `newText`.
- If the current text starts with `baseText` (someone only appended), write `newText`, a newline,
  then the appended tail, and report that the tail was re-attached.
- Otherwise refuse with a clear message ("Notes changed since you opened them; reload") and change
  nothing.

MCP `update_action` already requires `asOf`, which handles its own conflicts. MCP replacing notes
uses `asOf`, and does not use `baseText`.

**Migration** (`src/db/v5.13.sql`; apply by hand per `CLAUDE.md` conventions).

```sql
ALTER TABLE action_next ADD COLUMN next_notes TEXT NULL;

SET SESSION group_concat_max_len = 1000000;
UPDATE action_next a
JOIN (SELECT action_next_id,
             GROUP_CONCAT(note_line ORDER BY note_date, action_next_note_id SEPARATOR '\n') AS txt
      FROM action_next_note GROUP BY action_next_id) n
  ON n.action_next_id = a.action_next_id
SET a.next_notes = n.txt;
```

- Keep `action_next_note` (and its mapping file, but no longer mapped from `ActionNext`) for one
  prod release as a safety copy.
- Drop it in Phase 3.
- Check the migrated text against the old `getNextNotes()` output with the `" - "` removed, on
  the local copy, before deploying.

---

### MCP / API changes

- **Action context `notes`** (every tool that returns actions through `McpActionContextSupport`):
  the stored text exactly, or `null` / `""` when there are none. No `" - "` prefixes. Update the
  descriptions that mention notes.
- **`apply_changes` `update_action`:**
  - Add `notes`, which replaces the whole text. Like every `update_action`, it requires `asOf`.
    `null` or `""` clears the notes.
  - Keep `addNote`, which appends via `ActionNotesService.append` and now reaches twins too.
  - Sending both `notes` and `addNote` in one change is a validation error.
  - `addNote` stays allowed on template-generated instances (it changes only that instance).
    `notes` should follow the same rule.
- **`create_action` `notes`:** unchanged in meaning, stored as typed.
- **`search_actions`:** the "include notes" search moves from the `ActionNextNote` subquery to
  `lower(a.nextNotes) like :text`.
- **Docs:** update the tool descriptions (`ApplyChangesTool`, `SearchActionsTool`, and any others
  that describe `notes`). Add a dated entry to `docs/MCP-Feedback.md` describing the format change
  (no prefixes, notes is one value, `update_action.notes` added).
- **Tests:** update `McpDiscoveryServiceTest` and `ActionNextTest`, which reference notes today.

---

### UI changes

- **Shared Edit box.** The Note field already edits the full text (`aeNote` in
  `js/action-editor.js`, `ActionEditorRenderer`).
  - Send the loaded text as `baseNote` with `editAction` / `CompleteActionEdit`, and save through
    `ActionNotesService.replace`.
  - Show a refused save inline (the box already has `#aeError`).
  - Rename the placeholder from "One note per line" to something like "Notes".
- **Focused Action Notes drawer** (`FocusedActionPageRenderer.renderNotesDrawer`, `faSubmitNote`,
  `faUpdateNotesRow`):
  - The drawer becomes a notepad: a tall textarea holding the whole text. It saves on Ctrl+Enter,
    on blur and on drawer close, and only when the text changed. Show a small "Saved" or error
    status.
  - Keep a one-line "add a note" input under it that appends.
  - The `n` shortcut still opens the drawer.
  - The context-strip Notes row shows the first non-blank line.
  - The count in "Notes (4)" no longer means entries. Show nothing, or the number of non-blank
    lines; pick one and keep it simple.
  - Add `FocusedActionServlet` endpoints for replace and append, both through the service. Remove
    `extractNoteLines` and `splitNoteLines`.
- **Dashboard.**
  - `DashboardNowColumnService.parseNotes` reads the text; split on newlines only for display.
  - `handleAddCurrentActionNote` uses `append`.
- **Mobile `ActionServlet`.** Both writers use `append`; the reader uses the text.
- **Weekly report, templates and AI-adopt.** Read or write the plain text.

**Do not change:** Quick Capture, the switch bar, the work form, automatic next-action selection,
or the shared Edit box beyond its Note field. UI that appears on several pages must stay one
shared implementation, never copies kept in sync by hand.

---

### Phases

**Phase 1: column, service and every reader and writer switched over.**
- Migration `v5.13.sql`, the `nextNotes` property, and `ActionNotesService` with `replace`
  (including the base-text check), `append` and twins.
- Switch every writer and reader in the tables above, plus the MCP changes and docs.
- Remove the `nextNoteEntries` mapping from `ActionNext.hbm.xml` and the `ActionNextNote` usages.
  Leave the table, `ActionNextNote.java`/`.hbm.xml` and the `hibernate.cfg.xml` entry until
  Phase 3, or remove the mapping too if nothing references it.
- UI stays visually the same except that notes show as typed, without `" - "`, and the Edit box
  sends `baseNote`.
- Unit-test the service: append on empty and non-empty text, replace with an unchanged base,
  replace when only an append happened, a refused replace, twins updated, line-ending
  normalization, and the length cap.

**Phase 2: notepad UI.** The Focused Action Notes drawer becomes the editable notepad with quick
append, and the strip row and count change as described above.

**Phase 3, after a prod release: cleanup.** Drop `action_next_note` (`v5.14.sql` or the next
number), and delete `ActionNextNote.java`, `ActionNextNote.hbm.xml` and the `hibernate.cfg.xml`
line.

---

### Verification

Test on the **local** deploy. The local database is a disposable daily copy of prod, so saving,
appending, deleting and creating test actions there is fine.

- Run the migration locally and compare a sample of migrated notes with the old output.
- Edit box: edit notes, save, reopen; blank lines, indentation and leading dashes survive.
- Twins: capture onto a linked project (one sentence creates the twin pair). Edit notes and append
  a note from the Edit box, the Notes drawer and MCP; both twins show the same text in the
  database.
- Concurrency: open the Edit box, append a note from another tab, save the Edit box. Your edit is
  kept and the appended line is re-attached. Then change the middle of the text in the other tab
  and save the first; it is refused with no change.
- MCP against the **local** endpoint (`http://localhost:8089/dandelion/api/v1/mcp`, see
  `docs/MCP-Codex-Setup.md`): `notes` has no prefixes, and `update_action.notes` replaces,
  `addNote` appends, and both together is rejected. `search_actions` finds text in notes.
- Weekly report, Dashboard now column and mobile action page still show notes.
- `mvn clean package` passes.

---

### Working notes for the agent

- Read `CLAUDE.md` first. Key points:
  - Tomcat 9 runs on port 8089. Deploy by copying `target/dandelion.war` into the webapps folder,
    then wait about 3 minutes; the footer shows the version.
  - Migrations are `src/db/vN.M.sql`, applied by hand.
  - DAOs live in the `doa` package.
  - Bump the pom version for each deployable build.
- **The `dandelion` MCP server configured in Claude Code points at production.** Do not use it to
  test. Use the local endpoint.
- The VS Code Java extension may lock the repo's `target/`. If `mvn clean` fails, copy `pom.xml`
  and `src/` to a scratch directory and build there.
- Local MySQL: `mysql -uroot -pgoldenroot dandelion`.
- A redeploy signs Nathan out of the local app. Never enter his password; ask him to sign in.
- Work on a branch, and stop for Nathan's review after each phase.
