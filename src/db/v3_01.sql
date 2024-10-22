ALTER TABLE bill_entry ADD COLUMN (action_id int);

ALTER TABLE project_action ADD COLUMN (next_time_actual int);

ALTER TABLE project_action ADD COLUMN (next_notes TEXT);