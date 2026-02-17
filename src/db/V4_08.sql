/*
Table: project_action_change_log

Purpose
-------
Rename the action id column to make the next-action context explicit.

Notes
-----
- The application now uses action_next_id in the model and mappings.
- This migration preserves existing data by renaming the column.
*/

ALTER TABLE project_action_change_log
    CHANGE COLUMN action_id action_next_id INT NOT NULL;

/*
Table: tracker_narrative

Purpose
-------
Add project/contact context and last-updated tracking.
*/

ALTER TABLE tracker_narrative
    ADD COLUMN project_id INT NULL,
    ADD COLUMN contact_id INT NULL,
    ADD COLUMN last_updated DATETIME NULL;

UPDATE tracker_narrative
SET project_id = 12,
    contact_id = 66747,
    last_updated = CURRENT_TIMESTAMP
WHERE project_id IS NULL
   OR contact_id IS NULL
   OR last_updated IS NULL;

ALTER TABLE tracker_narrative
    MODIFY project_id INT NOT NULL,
    MODIFY contact_id INT NOT NULL,
    MODIFY last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE tracker_narrative
    ADD INDEX idx_tracker_narrative_contact_updated (contact_id, last_updated),
    ADD INDEX idx_tracker_narrative_project (project_id);
