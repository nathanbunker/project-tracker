-- v4.17: Project Issue capture
-- Adds the project_issue table for lightweight issue tracking against projects.
-- Supports BLOCKER / UNKNOWN / NOTE types and OPEN / RESOLVED status.

CREATE TABLE project_issue (
    project_issue_id  INT          NOT NULL AUTO_INCREMENT,
    project_id        INT          NOT NULL,
    issue_text        VARCHAR(1200) NOT NULL,
    issue_type        VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    issue_status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_date      DATETIME     NOT NULL,
    updated_date      DATETIME     NOT NULL,
    resolved_date     DATETIME     NULL,
    PRIMARY KEY (project_issue_id),
    INDEX idx_pi_project   (project_id),
    INDEX idx_pi_status    (issue_status),
    INDEX idx_pi_created   (project_id, issue_status, created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Extend project metadata capture for future Project Health reporting.
-- success_criteria_text is intentionally stored as raw newline-separated text for now.
ALTER TABLE project
    ADD COLUMN outcome_text TEXT NULL,
    ADD COLUMN success_criteria_text TEXT NULL;

-- Persist the exact prompt payload used for tracker narrative generation so it can be inspected.
ALTER TABLE tracker_narrative
    ADD COLUMN prompt_used_text TEXT NULL;

INSERT INTO project_next_action_type (next_action_type, next_action_label)
VALUES ('WOULD_LIKE_TO', 'I would like to');

DROP TABLE IF EXISTS project_action;

UPDATE project SET phase_code = 'Acti' where phase_code = 'Unkn';

-- Normalize legacy phase workflow states to Active and simplify the supported phase set.
UPDATE project
SET phase_code = 'Acti'
WHERE phase_code IN ('1CON', '2DOC', '3ACC', '4SET', '5TES', '6FEE', '7REP');

DELETE FROM project_phase
WHERE phase_code IN ('1CON', '2DOC', '3ACC', '4SET', '5TES', '6FEE', '7REP');

INSERT INTO project_phase (phase_code, phase_label)
SELECT 'Paus', 'Paused'
WHERE NOT EXISTS (SELECT 1 FROM project_phase WHERE phase_code = 'Paus');

INSERT INTO project_phase (phase_code, phase_label)
SELECT 'Comp', 'Complete'
WHERE NOT EXISTS (SELECT 1 FROM project_phase WHERE phase_code = 'Comp');