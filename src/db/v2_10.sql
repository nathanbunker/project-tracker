ALTER TABLE project ADD COLUMN priority_level INT NOT NULL DEFAULT 0;

ALTER TABLE project_action ADD COLUMN priority_level INT NOT NULL DEFAULT 1;
