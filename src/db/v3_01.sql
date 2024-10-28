ALTER TABLE bill_entry ADD COLUMN (action_id int);

ALTER TABLE project_action ADD COLUMN (next_time_actual int);

ALTER TABLE project_action ADD COLUMN (next_notes TEXT);

ALTER TABLE project_action ADD COLUMN (next_summary TEXT);

ALTER TABLE project_action MODIFY COLUMN next_action_type VARCHAR(16);
ALTER TABLE project_next_action_type MODIFY COLUMN next_action_type VARCHAR(16);

ALTER TABLE project_action ADD COLUMN (template_type VARCHAR(1));
UPDATE project_action SET template_type = 'D' WHERE next_action_type = 'G';

ALTER TABLE project_action RENAME COLUMN goal_action_id TO template_action_id;


UPDATE project_action SET next_action_type = 'WILL' WHERE next_action_type = 'D';
UPDATE project_action SET next_action_type = 'MIGHT' WHERE next_action_type = 'M';
UPDATE project_action SET next_action_type = 'WILL_CONTACT' WHERE next_action_type = 'C';
UPDATE project_action SET next_action_type = 'WILL_MEET' WHERE next_action_type = 'B';
UPDATE project_action SET next_action_type = 'COMMITTED_TO' WHERE next_action_type = 'T';
UPDATE project_action SET next_action_type = 'WILL' WHERE next_action_type = 'G';
UPDATE project_action SET next_action_type = 'FOLLOW_UP' WHERE next_action_type = 'F';
UPDATE project_action SET next_action_type = 'WAITING' WHERE next_action_type = 'W';
UPDATE project_action SET next_action_type = 'OVERDUE_TO' WHERE next_action_type = 'O';
UPDATE project_action SET next_action_type = 'GOAL' WHERE next_action_type = 'K';

ALTER TABLE project_action RENAME COLUMN task_status TO goal_status;

ALTER TABLE project_action ADD COLUMN (next_feedback TEXT);

ALTER TABLE project_action ADD COLUMN (priority_special VARCHAR(1));
