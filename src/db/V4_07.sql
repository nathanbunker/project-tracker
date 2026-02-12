/*
Table: tracker_narrative

Purpose
-------
Stores generated markdown narratives (daily, weekly, monthly) for the
Project Tracker system, including draft and final content plus lifecycle
state and metadata.

Design Intent
-------------
- Captures automated narrative output independently of project actions.
- Keeps generated text separate from approved text for review workflows.
- Supports period-based queries for reporting and auditing.
- Allows model and prompt metadata to be recorded when available.
- Avoids foreign keys to keep ingestion and migration lightweight.
*/

CREATE TABLE tracker_narrative (
    narrative_id        INT AUTO_INCREMENT PRIMARY KEY,
    display_title       VARCHAR(255) NOT NULL,
    narrative_type      VARCHAR(20) NOT NULL,
    period_start        DATE NOT NULL,
    period_end          DATE NOT NULL,
    review_status       VARCHAR(20) NOT NULL,
    markdown_generated  MEDIUMTEXT NULL,
    markdown_final      MEDIUMTEXT NULL,
    date_generated      DATETIME NULL,
    date_approved       DATETIME NULL,
    prompt_version      VARCHAR(50) NULL,
    model_name          VARCHAR(50) NULL,

    INDEX idx_type_period (narrative_type, period_start, period_end),
    INDEX idx_review_status (review_status),
    INDEX idx_generated (date_generated)
) ENGINE=InnoDB;
