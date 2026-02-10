CREATE TABLE project_action_taken (
  action_taken_id      INT NOT NULL AUTO_INCREMENT,
  project_id           INT NOT NULL,
  contact_id           INT NOT NULL,
  action_date          DATETIME NOT NULL,
  action_description   VARCHAR(12000) NOT NULL,
  provider_id          VARCHAR(30) NOT NULL DEFAULT '1',
  PRIMARY KEY (action_taken_id),
  KEY idx_pat_project_date (project_id, action_date),
  KEY idx_pat_contact_date (contact_id, action_date)
);

CREATE TABLE project_action_next (
  action_next_id         INT NOT NULL,
  project_id             INT NOT NULL,
  contact_id             INT NOT NULL,
  provider_id            VARCHAR(30) NOT NULL DEFAULT '1',
  next_action_status     VARCHAR(1) NOT NULL,
  next_change_date       DATETIME NULL,
  next_description       VARCHAR(1200) NOT NULL,
  next_due               DATETIME NULL,
  next_action_type       VARCHAR(16) NULL,
  next_time_estimate     INT NULL DEFAULT 0,
  next_contact_id        INT NULL,
  priority_level         INT NOT NULL DEFAULT 1,
  next_deadline          DATETIME NULL,
  goal_status            VARCHAR(1) NULL,
  template_action_next_id INT NULL,
  link_url               VARCHAR(1200) NULL,
  next_time_actual       INT NULL,
  next_notes             TEXT NULL,
  next_summary           TEXT NULL,
  template_type          VARCHAR(1) NULL,
  next_feedback          TEXT NULL,
  priority_special       VARCHAR(1) NULL,
  PRIMARY KEY (action_next_id),
  KEY idx_pan_project_due (project_id, next_due),
  KEY idx_pan_project_deadline (project_id, next_deadline),
  KEY idx_pan_contact (contact_id),
  KEY idx_pan_next_contact (next_contact_id),
  KEY idx_pan_status (next_action_status)
);


-- TAKEN: copy everything that has some action_description content
INSERT INTO project_action_taken (
  project_id, contact_id, action_date, action_description, provider_id
)
SELECT
  project_id, contact_id, action_date, action_description, provider_id
FROM project_action
WHERE action_description IS NOT NULL AND LENGTH(TRIM(action_description)) > 0;

-- NEXT: copy rows that have any meaningful next signal
INSERT INTO project_action_next (
  action_next_id, project_id, contact_id, provider_id,
  next_action_status, next_change_date, next_description, next_due,
  next_action_type, next_time_estimate, next_contact_id, priority_level,
  next_deadline, goal_status, template_action_next_id, link_url,
  next_time_actual, next_notes, next_summary, template_type,
  next_feedback, priority_special
)
SELECT
  action_id, project_id, contact_id, provider_id,
  next_action_status, next_change_date, next_description, next_due,
  next_action_type, next_time_estimate, next_contact_id, priority_level,
  next_deadline, goal_status, template_action_id, link_url,
  next_time_actual, next_notes, next_summary, template_type,
  next_feedback, priority_special
FROM project_action
WHERE
  (next_description IS NOT NULL AND LENGTH(TRIM(next_description)) > 0);

ALTER TABLE bill_entry
  CHANGE action_id action_next_id INT NULL;

DELETE FROM bill_entry
WHERE bill_mins = 0;

UPDATE bill_entry be
LEFT JOIN project_action_next pan
  ON be.action_next_id = pan.action_next_id
SET be.action_next_id = NULL
WHERE be.action_next_id IS NOT NULL
  AND be.action_next_id <> 0
  AND pan.action_next_id IS NULL;

ALTER TABLE project_action_change_log
  CHANGE action_id action_next_id INT NOT NULL;

ALTER TABLE project_action_proposal
  CHANGE action_id action_next_id INT NULL;

DROP TABLE project_action;

-- Validation queries
SELECT
  SUM(action_description IS NOT NULL AND LENGTH(TRIM(action_description)) > 0) AS taken_rows,
  SUM(next_description IS NOT NULL AND LENGTH(TRIM(next_description)) > 0) AS next_rows
FROM project_action;

SELECT COUNT(*) AS bill_entry_nulled
FROM bill_entry
WHERE action_next_id IS NULL
  AND bill_mins > 0;

SELECT COUNT(*) AS bill_entry_linked
FROM bill_entry be
JOIN project_action_next pan
  ON be.action_next_id = pan.action_next_id
WHERE be.action_next_id IS NOT NULL;

UPDATE project_action_next
SET next_action_status = 'R'
WHERE next_action_status = '';

