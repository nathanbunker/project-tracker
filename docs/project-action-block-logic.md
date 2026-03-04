# Project Action Block Logic

## Overview

Project actions now support dependency blocking within `ProjectActionNext`.

- Action **A** can be blocked by action **B**.
- While blocked, **A** is not actionable (its due date is cleared).
- When **B** is completed or closed, **A** is automatically unblocked and scheduled for today.

This supports workflows where a current action is paused until prerequisite work is finished.

## Data Model

### Database

Migration: `src/db/v4_11.sql`

- Adds nullable column: `project_action_next.blocked_by_id`
- Adds index: `idx_pan_blocked_by_id`

`NULL` means the action is not blocked.

### Hibernate / Model Mapping

`ProjectActionNext` now has a self-reference:

- Property: `blockedBy`
- Getter/Setter: `getBlockedBy()` / `setBlockedBy(...)`
- Hibernate mapping uses `blocked_by_id` as a nullable `many-to-one` to `ProjectActionNext`

## Web Workflow (ProjectActionServlet)

## Schedule and Block

When user selects **Schedule and Block** on action **A**:

1. New action **B** is created from Next Action sentence input.
2. `A.blockedBy = B`
3. `A.nextActionDate = NULL`
4. Action focus stays on **B** (the newly created blocker), so user can immediately start or reschedule it.

## Visibility in UI

### Left column (project action lists)

A **Blocked** section is shown after **Today** if blocked actions exist for the current project.

- Hidden when no blocked actions exist.
- Items use the same link/edit style as other action lists.

### Middle column (current action view)

A **Blocks** section appears before **Notes** when the current action blocks other actions.

- Lists actions where `blocked_by_id = current_action_id`
- Uses the same bullet/link/edit pattern as existing lists
- Hidden when none exist

## Unblocking Behavior

When blocker action **B** transitions to completed/closed:

1. Query all actions **A** where `A.blocked_by_id = B.id`
2. For each **A**:
   - Clear blocker: `A.blocked_by_id = NULL`
   - Reschedule: `A.next_action_date = today` (date-normalized via `WebUser` calendar)
   - Update change timestamp

This is implemented in `ProjectActionBlockerManager.unblockActionsBlockedBy(...)` and invoked from completion/close paths.

## Focus Behavior After Completion

`ProjectActionServlet` now prefers focus on the first unblocked action after a completion/close event.

- If completion unblocks actions, first unblocked action becomes current `completingAction`.
- If none were unblocked, existing fallback selection logic applies.

## Mobile Workflow

### Mobile Action view quick entry

The mobile single-action view includes a **Blocking action** quick entry:

- One description input
- **Schedule and Block** button

On submit for action **A**:

1. Create blocker action **B** with:
   - description from input
   - date = today
   - status = `READY`
   - billable resolved from project
2. Set `A.blockedBy = B`
3. Set `A.nextActionDate = NULL`
4. Redirect back to action view

## Completion Paths Covered

Unblock-on-completion is wired into both web and mobile completion/close flows, including:

- Web `ProjectActionServlet` completion/close handlers
- Mobile action/todo/project completion handlers

## Idempotency Notes

Unblocking logic is naturally idempotent:

- Only actions currently pointing to blocker **B** are updated.
- Re-running completion after blockers are already cleared finds no matching rows.

## Operational Notes

- Run DB migration `v4_11.sql` before deploying logic depending on `blocked_by_id`.
- The feature does not send notifications; it only updates scheduling/focus behavior.
