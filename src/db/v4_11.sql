ALTER TABLE project_action_next
    ADD COLUMN blocked_by_id INT NULL;

ALTER TABLE project_action_next
    ADD INDEX idx_pan_blocked_by_id (blocked_by_id);
