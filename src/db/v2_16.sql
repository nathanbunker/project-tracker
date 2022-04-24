ALTER TABLE project_action ADD COLUMN (goal_action_id int default 0);

CREATE INDEX project_action_goal ON project_action (goal_action_id);