/*
Table: project_action_next

Purpose
-------
Add time_slot column to support personal day-part grouping.

Notes
-----
- time_slot is nullable and stores enum names (e.g., 'MORNING', 'AFTERNOON').
- No data migration; all rows default to NULL.
- Will be used later by UI for grouping actions by time of day.
*/

ALTER TABLE project_action_next
    ADD COLUMN time_slot VARCHAR(20) NULL;
