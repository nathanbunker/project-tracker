/*
Add completion_order to support deterministic daily execution ordering.

Rules:
- Applies to READY actions due today or earlier (overdue), per provider + contact context.
- 0 means not yet prioritized for the current daily ordering pass.
*/

ALTER TABLE project_action_next
    ADD COLUMN completion_order INT NOT NULL DEFAULT 0;

ALTER TABLE project_action_next
    ADD INDEX idx_pan_provider_contact_date_order (provider_id, contact_id, next_action_date, completion_order);
