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
