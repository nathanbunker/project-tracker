# Billing v5.6 Implementation Note

Files changed:
- Added `src/db/v5.6.sql` and updated schema snapshots in `src/db/schema.sql` and `src/db/schema.local.sql`.
- Added new billing allocation models, Hibernate mappings, DAOs, services, DTOs, and unit tests.
- Updated existing `BillCode`, `BillEntry`, `Project`, Hibernate mappings, and `TimeTracker` budget-copy behavior.

Database changes:
- Added `bill_funding_source`, `bill_plan`, and `bill_plan_target` tables.
- Added nullable `funding_source_id` to `bill_code`.
- Added nullable `bill_budget_id` to `bill_entry` and `project`.
- Kept MyISAM tables and existing `bill_day`/`bill_month` designs unchanged.

Compatibility decisions:
- `billable` behavior is unchanged and still means work vs. personal activity.
- New columns are nullable and do not alter existing entry creation, reporting, or billing-code history when left null.
- New entry creation now copies the project's current `bill_budget_id` the same way existing logic copies the current billing code.
- Historical `bill_entry.bill_code` and `bill_entry.bill_budget_id` remain authoritative and are not retroactively changed.

Deferred work:
- No allocation dashboard UI was added.
- No seed data for funding sources or plans was added.
- No foreign-key enforcement or MyISAM-to-InnoDB migration was introduced.

Assumptions made:
- Historical plan retrieval should remain date-based, so prior approved plans are retained and newer approvals may mark older versions as `SUPERSEDED` without deleting them.
- Non-working capacity is represented by `bill_expected.work_status = 'N'`; other statuses continue to count as worked capacity unless current application logic narrows that later.