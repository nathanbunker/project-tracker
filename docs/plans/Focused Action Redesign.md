## Plan: Focused Action page redesign (drawers + meeting arcs)

The Focused Action page (`FocusedActionServlet`) is the surface Nathan works on all day. This plan
reduces what the left panel shows at all times and moves detail behind click-to-open drawers, and
it adds upcoming meetings to the focus clock. Decisions below were made with Nathan; treat them as
settled. Make changes carefully: each phase should be deployable on its own and must not change
behavior outside its scope.

**Key files**
- `src/main/java/org/dandeliondaily/servlet/FocusedActionServlet.java`: request dispatch, data
  loading (`loadTodayMeetingOptions`, `buildCycleBarData`, `extractNoteLines`,
  `handleSwitchAction`, `handleSelectMeetingAction`, `handleRefreshFocusClock`).
- `src/main/java/org/dandeliondaily/focus/render/FocusedActionPageRenderer.java`: all HTML, CSS and
  JS for the page (println-based). Clock code is `faDrawClock`, `faAddSector`, `faDrawFace`,
  `faDrawMinuteHand`; page state is the `faState` JS object.
- `DashboardCurrentActionService` (`buildOrderedCycleList`, `selectNextAfterCurrentInList`,
  `ensureCurrentActionSelected`, `isMeetingAction`).
- `LinkedMeeting` / `LinkedMeetingDao.findByAction`: exact meeting times
  (`scheduledStartEpochMs`, `scheduledEndEpochMs`, `joinUrl`, `sourceStatus`). See
  `docs/linked-meeting-domain-model.md`. Meetings created by hand in Dandelion have no linked row,
  so they have no time.

**Do not change**
- The top switch bar (recent-actions slots) and Quick Capture box. Keep them exactly as they are.
- The work form ("What action was taken" / status / Next).
- The automatic next-action selection (`buildOrderedCycleList` used by
  `selectNextAfterCurrentInList` and `ensureCurrentActionSelected`).

---

### Overall pattern

The left panel becomes: title card, work form (unchanged), then a **context strip** of four
one-line rows. Each row shows a count and a one-line preview, and clicking it opens a drawer:

```
Notes (7)          "Called Jen, waiting on spec..." (latest)    >
Meetings (3)       Next 2:00 Standup, in 25m  · 1 to wrap up     >
Up next (4 of 14)  Project X - Draft the reply (15m)            >
This project (5)   2 today · 3 later                             >
```

- **Drawer:** slides over the left column only, so the clock stays visible. One drawer open at a
  time, its own scroll, Esc closes it. Do not use the full-page `fa-modal-overlay` for this.
- **Shared action picker:** Meetings, Up next and This project are the same list component with
  different data. Put it in its own renderer class (like `EditActionModalRenderer`) rather than
  growing `FocusedActionPageRenderer`.
  - Row shows: project, description, estimate, minutes spent today, time or date label, and a
    marker on the current action.
  - Opens scrolled to the current item. Filter box at the top; arrow keys and Enter work.
  - Action rows call the existing `switchAction` (keeps "clock follows the switch").
  - Meeting rows call the existing `selectMeetingAction` (keeps the pre-meeting memory: after the
    meeting is completed, Dandelion returns to the action that was current before).
- Lists are small; render them server-side with the page, as the page already reloads on every
  switch.

---

### Phase 1: drawer, context strip, Notes

1. Build the drawer and context strip.
2. **Notes row (collapsed):** note count and the latest note, cut to one line. With no notes, a
   faint "Add note" link.
3. **Notes drawer:** all notes **oldest first** (current `extractNoteLines` order), add box at the
   bottom, focused when the drawer opens. Enter saves through the existing `addCurrentActionNote`
   call; the drawer stays open so several notes can be added.
4. A keyboard shortcut (e.g. `n`, ignored while typing in an input) opens the drawer at the box.
5. Remove the always-visible note input. Move **Reschedule** and **Edit Action** (currently in
   `fa-note-actions` next to the note box) into the title card as small buttons.

### Phase 2: Up next drawer

1. **Collapsed:** the next item and its position ("4 of 14"), no arrows.
2. **Drawer:** the full `buildOrderedCycleList` queue, grouped **Overdue** / **Today**, current
   item highlighted.
3. Remove the prev/next arrows and the `hintActionId` stepping logic (`faCyclePrev`,
   `faCycleNext`, `faRenderCycleBar`, `PARAM_HINT_ACTION_ID` handling) once nothing uses them.
4. If the current action is not in today's queue (see Phase 4), the collapsed row shows the first
   queue item with a small "current action is not in today's list" marker instead of "N of M".
5. **Optional, last step of this phase: overdue meetings.** Overdue meetings belong with overdue
   actions, not in the Meetings drawer. `buildOrderedCycleList` removes all meetings, and that list
   also drives automatic next-action selection, so **do not change it**. Instead, add READY
   meetings dated before today to the drawer's Overdue group as display-only rows with a meeting
   marker. Clicking one uses `selectMeetingAction`, so after wrapping it up Nathan lands back on
   what he was doing.

### Phase 3: Meetings drawer and clock arcs

**Shared helper.** Write one helper that loads today's meetings with their `LinkedMeeting` times.
Both the drawer row's "Next" and the clock use it, so they always agree.
- Scope: `WILL_MEET`, status READY, dated **today only** (as `loadTodayMeetingOptions` does now).
  Overdue meetings are not shown here.
- Skip meetings whose `LinkedMeeting.sourceStatus` is cancelled.
- Closed meetings drop out because they are no longer READY.

**Meetings row (collapsed):** count and the next meeting's time and title, with "in 25m" or "now".
Add "N to wrap up" when there are ended-but-open meetings.

**Meetings drawer:** today's meetings in time order, in four states. Nathan closes a meeting only
after recording next steps, so an ended-but-open meeting is a to-do, not history.

| State | Look | Click |
|---|---|---|
| Ended, not closed | Greyed, labeled "ended 10:30" | Still selectable to record next steps and close |
| In progress | Highlighted | Selects it |
| Upcoming | Normal, time and "in 25m" | Selects it |
| No time (no linked row) | At the bottom | Selects it |

Show a join link when `joinUrl` is set.

**Clock arcs.** The dial already shows the next 60 minutes: the hand is the current minute and the
green wedge is remaining estimate ahead of it. Purpose of the arcs: show whether the coming hour is
committed, so Nathan sees hard stops and doesn't miss commitments.
- Draw **every** timed, non-cancelled meeting that has not ended and starts within **58 minutes**.
  Nothing beyond 58 minutes. Clip each arc's end at now + 58 so it never wraps around to the hand.
- Each meeting is a **band on the outer rim** (just outside or inside the tick ring), not a filled
  wedge, so it never hides the green estimate. Overlap with the estimate is a useful warning.
- Trim about half a minute off each end of each arc so back-to-back meetings show a visible gap.
- In progress: arc from the hand to the meeting's end, in a stronger shade.
- Ended meetings (even if not closed): no arc.
- Overlapping (double-booked) meetings: second, inner lane.
- Untimed meetings: not on the dial.
- Draw the arcs whether or not the timer is running.
- **Time zone:** compute minutes-until from the server's current time, not the browser's.
  `faState.nowMinute` comes from the user's time zone, so a browser in another zone would misplace
  arcs. Send `meetingArcs: [{startEpochMs, endEpochMs}]` plus `serverNowEpochMs` in `faState` and
  in the `refreshFocusClock` JSON response so arcs update as time passes.

### Phase 4: This project drawer

1. **First, verify switching to a future-dated action.** Decision: it becomes current and **its
   date is left alone** (Nathan does this to add notes and leave it for later). Check that
   `ensureCurrentActionSelected` and the post-work flow do not swap a current action back out
   because it isn't due today. If they do, fix that logic; do not change the action's date.
2. New query: READY actions in the current action's project, assigned to the user, dated today or
   later, plus overdue. Grouped **Overdue / Today / Tomorrow / Later**, plus a small **Done today**
   section.
3. Per CLAUDE.md, the UI and MCP should share services. If MCP `search_actions` already has a
   project-scoped query, both should call the same service.
4. Rows switch with `switchAction`.

---

### Verification

- Deploy per CLAUDE.md (`mvn clean package`, confirm changed classes in the WAR, copy to Tomcat,
  wait about 3 minutes, check the footer version). Bump `<version>` in `pom.xml` for each
  deployable phase.
- Test on the local deploy (http://localhost:8089/dandelion/), not via the `dandelion` MCP server,
  which points at production.
- For each phase, confirm: the switch bar and Quick Capture are unchanged; the work form and
  automatic next-action selection behave as before; the clock still shows spent/remaining/overrun.
- Phase 3: check arcs with back-to-back, overlapping, in-progress and 58+ minute meetings, with the
  timer both running and stopped.
