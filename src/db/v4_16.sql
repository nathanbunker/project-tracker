-- v4_16.sql
-- Add workflow_type to web_user.
-- Controls UI/presentation mode (terminology, navigation, scheduling features).
-- This is NOT a permissions change and does not alter user_type behavior.
-- Existing users automatically receive STANDARD via the column default.

ALTER TABLE web_user
  ADD COLUMN workflow_type ENUM('STANDARD', 'STUDENT') NOT NULL DEFAULT 'STANDARD'
  AFTER user_type;
