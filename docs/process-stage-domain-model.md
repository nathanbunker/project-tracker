# Process Stage — Domain Definition and Usage

## Purpose

The `process_stage` field represents the **structural phase of a workday** in which a work-related action belongs.
It replaces the legacy `priority_special` name to better reflect meaning without changing existing behavior or values.

This field applies **only to work actions**, not personal planning.

---

## Conceptual Meaning

`process_stage` answers a single question:

> *Where does this action fit within the flow of the workday?*

It is **not** a measure of:

* urgency
* importance
* scheduling date
* personal routine placement

Those concerns are handled by other fields:

* **Calendar day** → `next_action_date`
* **Personal time-of-day placement** → `time_slot`
* **General ordering within work** → standard priority logic

---

## Current Values (Unchanged)

At present, `process_stage` contains **four existing values** inherited from `priority_special`.
These values and their behavior remain unchanged in this phase of the refactor.

Future revisions may redefine or simplify these values, but **no semantic or behavioral change is introduced now**.

---

## Scope of Use

### Included

* Structuring the **beginning, core execution, and closeout** of the workday
* Ensuring critical workflow steps appear in the correct daily sequence
* Supporting deterministic ordering of work actions

### Excluded

* Personal task organization
* Time-of-day grouping
* Reporting or billing logic
* Calendar scheduling

This separation keeps the domain model clear and prevents the historical overloading of “priority.”

---

## Relationship to Other Planning Fields

The action model is now intentionally divided into **three independent axes**:

* **`next_action_date`** → *Which day the action appears*
* **`process_stage`** → *Where it belongs in the workday lifecycle*
* **`time_slot`** → *When during the day a personal action occurs*

These fields must remain **semantically independent**.

---

## Design Philosophy

Renaming to `process_stage` clarifies intent without altering behavior.
This is a **stabilization step** in a broader refactoring that separates:

* workflow structure
* calendar scheduling
* personal routine planning

Maintaining this separation allows the system to evolve safely while remaining intuitive to use.

---

## Summary

`process_stage` defines the **position of a work action within the daily work lifecycle**.
It is a structural concept, not a priority or schedule.

This rename preserves existing behavior while establishing a clearer, more maintainable domain foundation for future improvements.
