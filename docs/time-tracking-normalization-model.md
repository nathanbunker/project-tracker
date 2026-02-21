# Time Tracking Normalization — Target Behavior Model

## Purpose

This document defines the **intended behavior** for time tracking edits and normalization.
It is a product/logic model used to drive test design and future code fixes.

It is **not** a statement that current code already satisfies every rule.

---

## Core Concepts

### Working Session

A working session is a continuous period where the user is at work and tracking activity.

Within a working session:

* entries should not overlap
* normalization should not create artificial gaps
* edits should reallocate time among neighboring entries when possible

Between working sessions:

* gaps are allowed and expected
* breaks in non-contiguous 10-minute blocks are preserved

### 10-Minute Time Blocks

Time is interpreted in 10-minute buckets for normalization behavior.

* session start rounds **down** to the nearest 10-minute boundary
* session end rounds **up** to the nearest 10-minute boundary
* break merge rules are based on bucket contiguity, not only raw minute difference

This block model is the primary mental model for understanding merge/split behavior.

---

## Invariants

### Invariants inside a working session

1. No overlap between entries.
2. No introduced gaps from normalization.
3. Time changes should prefer **reallocation** before changing total session time.
4. No timestamp should ever be adjusted into the future.

### Session boundary behavior

1. If an edit pushes outside session start/end boundaries, total session time can change.
2. Shortening a middle entry should generally transfer time to adjacent entries.
3. Lengthening an entry should cannibalize adjacent entries as needed.

---

## Two Explicit Phases

### Phase 1: Edit Adjustment (Local Reallocation)

User edits one entry (start/end), and the system adjusts surrounding entries in that day context.

Intent:

* keep timeline valid
* avoid overlap
* preserve continuity for contiguous neighbors
* redistribute time to/from neighboring entries where possible

Key expectations:

* shortening an edited entry gives time back to sticky neighbors
* lengthening an edited entry takes time from neighbors (possibly multiple)
* direction is user-controlled through edited boundaries:

	* moving start earlier cannibalizes from previous entries
	* moving end later cannibalizes from later entries
	* user specifies start/end, not a direct "total minutes" target
* if no neighbor time exists to absorb a change, boundary effects can change total time

### Phase 2: Daily Normalization (Session Cleanup + Rounding)

After (or independent of) editing, daily entries are normalized to the bucket model.

Intent:

* snap session edges to 10-minute boundaries
* preserve true breaks between non-contiguous block ranges
* merge/close micro-gaps according to bucket rules
* enforce non-overlap and clean chronological consistency

---

## Break and Merge Rules (Bucket-Based)

Breaks are evaluated by 10-minute bucket relationships.

* If end/start points land in the same or contiguous bucket behavior, break may disappear.
* If end/start points land in non-contiguous buckets, break is preserved.

Example:

* stop 1:32 → rounds to 1:40
* restart 1:57 → rounds to 1:50
* resulting preserved break: 10 minutes

So a raw break >20 minutes often maps to non-contiguous buckets and remains visible.

---

## Current Activity and Ragged End Rule

For **today**, the last (current) activity should remain ragged.

Rules:

* do not round it into the future
* do not force a synthetic future end time
* allow zero or exact-minute end while it is current activity

This is a hard safety requirement.

---

## Zero-Minute Entries

Zero-minute entries are considered noise and should be removed completely.

Target behavior:

* remove any entry whose normalized start and end are equal
* keep the timeline in a clean, meaningful state without zero-duration artifacts

---

## Billable vs Non-Billable

All tracked activity participates in timeline integrity and normalization.

In this document, roll-up/reporting totals are out of scope.

Behavior semantics:

* billable (`Y`) and non-billable (`N`) both occupy real timeline time
* non-billable entries are not breaks and should not trigger special break rounding behavior
* non-billable entries still participate in overlap/gap resolution and reallocation logic

This distinction is essential: billing is a reporting attribute, not a timeline integrity attribute.

---

## Test-Driven Implications

Test design should validate the target model, including edge conditions where existing code may differ.

Priority scenario families:

1. middle-entry shrink/expand with sticky neighbors
2. cannibalization across multiple neighbors
3. session boundary extension/reduction effects
4. bucket-based break merge vs preserve behavior
5. today/current-entry ragged end safety (never future-rounded)
6. non-billable participation in redistribution and break continuity rules
7. zero-minute cleanup behavior with immediate removal

---

## Scope Note

This document describes **desired behavior** and reasoning rules.
Where implementation diverges, tests based on this model should drive corrective changes.
