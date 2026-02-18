/*
Table: project_action_next

Purpose
-------
Add billable flag to next actions and backfill from project bill codes.

Notes
-----
- billable is sourced from bill_code.billable via project.bill_code.
- Defaults to 'N' when bill_code is missing.
*/

ALTER TABLE project_action_next
    ADD COLUMN billable CHAR(1) NULL;

UPDATE project_action_next pna
JOIN project p ON p.project_id = pna.project_id
LEFT JOIN bill_code bc ON bc.bill_code = p.bill_code
SET pna.billable = COALESCE(bc.billable, 'N')
WHERE pna.billable IS NULL;

ALTER TABLE project_action_next
    MODIFY COLUMN billable CHAR(1) NOT NULL DEFAULT 'N';
