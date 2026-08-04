# Billing and Allocation Conceptual Framework

## Purpose

This document describes the conceptual model used by the time-tracking and billing system.

The system supports two ways of managing work at the same time:

1. **Percentage-based allocation management** — used for work such as the CDC cooperative agreement and AIRA-funded activities.
2. **Fixed-hour commitments** — used for contracts or other work where a specific number of hours is authorized and should not be exceeded.

The system continues to track work at the project and time-entry level, while allowing that detailed activity to roll up into management and funding views.

> Track work at the level where it is actually performed, while preserving the funding and planning context needed to explain where the time belongs and whether actual effort is aligned with the plan.

---

# 1. Core Concepts

## 1.1 Work vs. Personal

The existing `billable` flag is a hard distinction between **work** and **personal** activity.

In this system, `billable` does **not** mean externally invoiced, contract funded, CDC funded, or chargeable to a client. It means the activity is part of the work day.

Examples of work include:

- CDC cooperative agreement work
- External contract work
- AIRA-funded strategic work
- Internal administration
- Potentially leave, if leave is later tracked in the system

This concept remains independent from funding and allocation.

## 1.2 Funding Source

A **funding source** identifies where the money supporting an activity ultimately comes from.

Examples:

- CDC Cooperative Agreement
- AIRA Diversified Funding
- KL&A Minnesota Contract
- AIRA Internal
- Other external funding

Funding sources answer:

> Who is ultimately funding this work?

Funding source types currently include:

- `FEDERAL`
- `AIRA`
- `CONTRACT`
- `INTERNAL`
- `OTHER`

A funding source does not directly receive time entries. Billing codes are associated with funding sources.

## 1.3 Billing Code

A **billing code** is the primary management and reporting bucket for time.

Examples:

- CDC – HL7 v2
- CDC – Emerging Standards
- CDC – Technical Assistance
- CDC – Measurement
- AIRA – Diversified Funding
- KL&A – Minnesota
- General Activities

The billing code answers:

> Under what current funding or management allocation was this work authorized?

Billing codes are the main bridge between detailed project work and management reporting.

A billing code may be:

- percentage-managed
- associated with a fixed-hour contract budget
- both percentage-managed and hour-capped
- simply used as a work classification

---

# 2. Projects and Historical Assignment

## 2.1 Project

A **project** represents the concrete thing being worked on.

Examples:

- Immunization Implementation Guide
- IGEG
- InteropHub Emerging Standards
- Signal/CLEAR
- IVC
- Minnesota MQE
- IFG
- Repository modernization

A project has a **current billing code** and may also have a **current contract budget**. These current assignments are copied to new time entries.

## 2.2 Time Entries Preserve History

When a time entry is created, the system copies the project's current billing code and, when applicable, contract budget onto the entry.

This preserves the funding context that existed when the work occurred.

Example:

| Period | Project | Billing Code |
|---|---|---|
| January–March | Signal/CLEAR | AIRA Diversified Funding |
| April–June | Signal/CLEAR | CDC Emerging Standards |
| July onward | Signal/CLEAR | CDC Measurement |

Changing the project's current billing code does **not** reclassify prior work.

The same project can therefore appear under multiple allocations in historical reports.

The time entry is the authoritative record of where the work was classified when it occurred.

---

# 3. Percentage-Based Allocation Planning

## 3.1 Allocation Plan

An **allocation plan** describes how a person's working capacity is expected to be distributed.

Example:

| Billing Code | Annual Target |
|---|---:|
| CDC – HL7 v2 | 35% |
| CDC – Emerging Standards | 25% |
| CDC – Technical Assistance | 15% |
| CDC – Measurement | 10% |
| AIRA Diversified Funding | 5% |
| General Activities | 10% |

The allocation plan is management direction, not a replacement for actual time records.

Actual time continues to come from time entries.

## 3.2 Annual Target

The **annual target** represents the intended overall allocation for the fiscal period.

It answers:

> Across the year, approximately how much of this person's work should support this allocation?

Annual targets are useful for:

- budget planning
- cooperative agreement alignment
- resource planning
- year-to-date comparison
- explaining how a position is funded

The annual target is the baseline against which the year is evaluated.

## 3.3 Steering Target

The **steering target** represents the direction management wants followed **now**.

It can differ from the annual target.

Example:

| Allocation | Annual Target | Current Steering Target |
|---|---:|---:|
| HL7 v2 | 35% | 42% |
| Emerging Standards | 25% | 20% |

This may happen because actual work has drifted from the annual plan or because organizational priorities changed.

The steering target answers:

> Given where things stand today, approximately how should upcoming work be distributed?

The annual target describes the plan for the year. The steering target describes current direction.

---

# 4. Effective-Dated Plan Versions

Allocation plans are versioned and effective-dated because management direction can change during the year.

Example:

## Version 1 — Effective October 1

| Allocation | Annual | Steering |
|---|---:|---:|
| HL7 v2 | 40% | 40% |
| Emerging Standards | 20% | 20% |

## Version 2 — Effective January 15

| Allocation | Annual | Steering |
|---|---:|---:|
| HL7 v2 | 35% | 42% |
| Emerging Standards | 25% | 18% |

The prior version is preserved.

This makes it possible to answer both:

- What was the plan originally?
- What direction was in effect at a particular point in time?

Approved plans should not normally be edited in place. A new version should be created when direction changes.

---

# 5. Variance Policies

When the annual target and current steering target differ, the system records how the steering direction should be interpreted.

## CARRY_FORWARD

Past variance should influence future direction.

Example:

- Annual HL7 v2 target: 35%
- Actual effort is below target
- Current steering target: 43%

The increased steering target is intended to help recover the accumulated difference.

## FORWARD_ONLY

The new direction applies prospectively.

Past differences are not expected to be recovered.

Example:

- Previous direction was 40%
- Leadership changes future direction to 35%
- Work from this point forward should be approximately 35%

## MANUAL

Leadership has chosen the steering percentage based on judgment rather than a calculated recovery rule.

These policies document why steering may differ from the annual plan.

---

# 6. Fixed-Hour Contract Budgets

Some work is controlled by a specific number of authorized hours rather than only by a percentage.

A **contract budget** represents that hour limit.

Example:

| Contract | Authorized | Used | Remaining |
|---|---:|---:|---:|
| Minnesota/KL&A | 240 hrs | 173 hrs | 67 hrs |

The contract budget answers:

> How many hours are authorized, how many have been consumed, and how many remain?

The authoritative usage is calculated from time entries associated with that budget.

A contract budget contains:

- billing code
- start date
- end date
- authorized minutes/hours

A project may be assigned to a current contract budget. New entries copy that budget ID. Historical entries retain their original budget assignment.

---

# 7. Percentage Allocation and Fixed-Hour Budgets Can Coexist

A contract can participate in both management models.

For example:

- Management may expect approximately 6% of annual work to support the Minnesota contract.
- The contract may separately authorize a maximum of 240 hours.

The system can therefore represent:

| Billing Code | Annual Target | Steering | Hour Budget |
|---|---:|---:|---:|
| KL&A – Minnesota | 6% | 6% | 240 hrs |

The percentage answers:

> How much of overall capacity should be going here?

The hour budget answers:

> How many contract hours are actually authorized?

These are complementary controls.

---

# 8. Diversified Funding

AIRA-funded diversified activity is treated as another percentage allocation when AIRA intentionally funds part of the employee's capacity for activities outside the CDC-funded scope.

Example:

| Allocation | Annual Target |
|---|---:|
| AIRA Diversified Funding | 5% |

Possible activities might include:

- business-development work
- exploratory projects
- strategic prototypes
- work supporting potential new funding
- limited non-CDC external collaboration

This provides an explicit place for work that AIRA wants performed but that should not automatically be attributed to CDC.

It should still be tracked through normal projects.

---

# 9. Shared and Administrative Work

Not every minute can reasonably be assigned to a substantive project.

Examples include:

- email triage
- daily planning
- weekly reporting
- mandatory HR activity
- general internal coordination

The goal is not to force artificial precision.

Preferred workflow:

1. Keep true triage and shared administrative work in an appropriate general billing code.
2. If an email or administrative action becomes substantive, create or use a project task and record the substantive work against the relevant project.
3. Keep the residual shared work visible rather than silently assigning it to unrelated projects.

The exact organizational accounting treatment of shared administrative work is separate from the detailed tracking model.

---

# 10. Expected Work Capacity

The system stores expected work time by date.

This provides the denominator needed for allocation planning and can account for normal scheduled work, non-working days, holidays, and other work-status information.

Conceptually:

```text
Expected work minutes × allocation percentage = target minutes
```

Example:

```text
1,500 expected work hours × 35% HL7 v2 = 525 target hours
```

Actual time can then be compared with target time.

---

# 11. Conceptual Reporting Model

The steering dashboard can combine planning and actual data.

For each allocation:

| Measure | Meaning |
|---|---|
| Annual target | Intended allocation for the year |
| Current steering target | Current management direction |
| Actual YTD | Actual recorded percentage |
| Target YTD | Expected amount based on plan |
| Variance | Difference between actual and target |
| Forecast | Expected year-end outcome |
| Required recovery rate | Future percentage needed to reach annual target |
| Projects | Work that produced the actual hours |

For fixed-hour budgets, additional measures include:

- authorized hours
- used hours
- remaining hours
- percentage consumed
- contract-period status

---

# 12. What the Model Does Not Do

The current framework deliberately does not:

- allocate time to objectives
- automatically decide whether work is allowable under a funding source
- automatically change project funding
- retroactively reclassify historical entries
- equate `billable` with invoiced work
- automatically alter steering targets
- force every project to remain under one billing code forever
- force percentage plans to describe every project individually

The system records management decisions and actual work. It does not replace those decisions.

---

# 13. Possible Future Objective Model

Objectives are not currently required for allocation tracking.

If added later, an objective would describe the funded result or obligation that projects support.

Conceptually:

```text
Funding Source
    ↓
Billing Code / Allocation
    ↓
Objective
    ↑
Projects
    ↓
Time Entries
```

A project could identify one or more objectives that it helps fulfill.

Time would still be recorded against projects and billing codes, not divided among objectives.

Objectives would primarily demonstrate:

> These projects collectively satisfy the work expected under this allocation.

This can be added later without changing the current time model.

---

# 14. Recommended Annual Setup Process

## Step 1 — Define funding sources

Review the organizations or funding mechanisms supporting the work.

Examples:

- CDC Cooperative Agreement
- AIRA Diversified Funding
- External contracts
- Internal AIRA funding

Create or update funding-source records as needed.

## Step 2 — Define billing codes

Determine the management buckets leadership wants to see.

Examples:

- CDC – HL7 v2
- CDC – Emerging Standards
- CDC – Technical Assistance
- CDC – Measurement
- AIRA Diversified Funding
- Contract – Minnesota
- General Activities

Assign each billing code to its appropriate funding source.

Billing codes should represent durable management/funding categories rather than individual projects.

## Step 3 — Define contract budgets

For work with an explicit hour authorization:

- create the budget
- assign the billing code
- enter the contract period
- enter authorized hours

Do not use a fixed-hour budget merely because leadership has a percentage target.

Budgets are for actual hour-limited commitments.

## Step 4 — Create the annual allocation plan

Enter:

- fiscal period
- initial effective date
- annual percentage for each allocation
- initial steering percentage
- target mode
- optional contract budget
- variance policy

The annual allocation should represent the best understanding of how the employee's capacity is funded and expected to be used.

## Step 5 — Review project assignments

For active projects:

- confirm the current billing code
- confirm the current contract budget when applicable

A project should be assigned according to how the **next unit of work** should be classified.

Do not change the project merely to make historical reports look different.

## Step 6 — Work normally

Continue tracking actual time against projects.

Time entries preserve the billing context that existed when work occurred.

## Step 7 — Review actual allocation periodically

Compare:

- annual target
- actual year-to-date percentage
- current steering target
- contract budget consumption

The purpose is steering, not enforcing exact weekly percentages.

## Step 8 — Revise direction when necessary

If leadership changes priorities:

- create a new allocation-plan version
- set a new effective date
- change annual and/or steering targets
- document the reason
- preserve the old version

If a project's funding changes:

- change its current billing code
- optionally change its contract budget
- leave historical entries unchanged

---

# 15. Planning Questions for a New Year

## Funding

1. What funding sources pay for this position?
2. Which sources are federal, AIRA-funded, contract-funded, or internal?
3. Are there activities that explicitly must not be attributed to CDC?

## Allocations

4. What broad work buckets does management want to see?
5. What annual percentage should each allocation receive?
6. Do the percentage allocations represent the entire expected work portfolio?
7. Are any percentages explicitly reserved for diversified or exploratory work?

## Contracts

8. Which activities have a hard contractual hour limit?
9. What are the contract start and end dates?
10. What is the authorized hour total?
11. Does management also want the contract represented as an approximate percentage of total capacity?

## Projects

12. Which active projects belong under each billing code today?
13. Are any projects likely to move between funding sources during the year?
14. Are current contract projects assigned to the correct hour budget?

## Steering

15. How frequently will actual allocation be reviewed?
16. When actual effort differs from plan, should the difference be recovered or accepted prospectively?
17. Who decides when annual or steering targets are changed?
18. What amount of variance should trigger a management conversation?

---

# 16. Mental Model

The simplest way to think about the system is:

```text
FUNDING SOURCE
Where does the money come from?
        ↓
BILLING CODE
What management/funding bucket is this work under?
        ↓
PROJECT
What concrete thing am I working on?
        ↓
TIME ENTRY
What did I actually spend time doing?
```

Alongside that operational chain:

```text
ALLOCATION PLAN
How should my overall work capacity be distributed?

CONTRACT BUDGET
How many hours am I authorized to spend on this specific contract?
```

The allocation plan steers the portfolio.

The contract budget limits specific work.

The project organizes actual work.

The time entry preserves what actually happened.

Together, these allow detailed personal time tracking to roll up into a management model suitable for cooperative-agreement, diversified-funding, and contract work.
