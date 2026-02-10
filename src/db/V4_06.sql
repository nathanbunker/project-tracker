ALTER TABLE project_narrative
  ADD COLUMN last_updated DATETIME NULL;

UPDATE project_narrative
SET last_updated = narrative_date
WHERE last_updated IS NULL;

ALTER TABLE project_narrative
  MODIFY last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
