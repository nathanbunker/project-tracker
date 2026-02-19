# Time Slot — Domain Definition and Usage

## Purpose

The `time_slot` field defines **when during a day** a personal action is intended to be performed.
It introduces a clear separation between:

* **Work lifecycle structure** (`process_stage`)
* **Personal day placement** (`time_slot`)

This prevents the historical confusion where a single “priority” field attempted to describe both **urgency** and **time-of-day context**.

The result is a cleaner and more durable domain model.

---

## Conceptual Meaning

`time_slot` answers a single question:

> *At what part of the day should this action occur?*

It does **not** describe:

* importance
* urgency
* duration
* scheduling date
* reminders or notifications

Those concerns are handled elsewhere:

* **Date** → `next_action_date`
* **Work sequencing** → `process_stage`
* **Sorting within work** → priority logic (work only)

---

## Enum Values

The system defines four explicit day-part values:

* `WAKE` — immediately after waking; morning routine before the main day begins
* `MORNING` — primary morning activity window (workday or weekend)
* `AFTERNOON` — post-lunch or later-day activity window; includes “after work”
* `EVENING` — night routine and pre-sleep activities

No additional values are defined.

This constraint keeps the mental model simple and stable.

---

## Null Semantics

`time_slot` is **nullable by design**.

A `NULL` value means:

> *This action is not anchored to a specific time of day.*

This is intentional and must be preserved.

### Rules

* Existing data remains `NULL`.
* The system **does not auto-populate** null values.
* Defaults may be applied **only at creation time** for new personal actions
  (current default: `AFTERNOON`).

Null is a **meaningful state**, not missing data.

---

## Relationship to Other Fields

### `next_action_date`

* Determines **which calendar day** the action appears.
* Stored as **DATE-only** (timezone-independent).

### `process_stage`

* Applies to **work actions only**.
* Represents lifecycle structure:

  * begin day
  * execute day
  * end day

### Separation Principle

These fields must remain **orthogonal**:

* `time_slot` → *when in the day*
* `process_stage` → *where in the work lifecycle*
* `next_action_date` → *which day*

No field should duplicate another’s meaning.

---

## Sorting and Display Rules

When grouping personal actions by `time_slot`, ordering must be **explicit and stable**:

1. `WAKE`
2. `MORNING`
3. `AFTERNOON`
4. `EVENING`
5. `NULL` (last)

Sorting must **not** rely on enum ordinal position.
Ordering must be defined intentionally in code to prevent future enum edits from breaking behavior.

Within each slot:

* overdue items first
* then by `next_action_date`
* then by stable tie-breaker (e.g., creation order or ID)

---

## Scope of Use

### Included

* Personal action planning
* Mobile Todo grouping
* Visual day-structure for routines

### Excluded

* Work execution ordering
* Billing or reporting
* Notifications or reminders
* Calendar recurrence logic

Future features may reference `time_slot`, but its **semantic meaning must not expand**.

---

## Design Philosophy

`time_slot` exists to model **how humans experience a day**, not how software schedules time.

Key principles:

* **Simple vocabulary**
* **Stable semantics**
* **Nullable by intent**
* **Independent of timezone**
* **Separated from urgency and priority**

This keeps the planning system intuitive while allowing the broader application to evolve safely.

---

## Summary

The introduction of `time_slot` completes the separation of concerns within the action model:

* **Date** → when it appears
* **Work stage** → where it fits in the workday
* **Time slot** → when during the day it belongs

This structure replaces the overloaded historical “priority” concept with a **clear, extensible, and human-aligned domain model**.
