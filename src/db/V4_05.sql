/*
Table: project_narrative

Purpose
-------
Stores short, structured narrative observations captured during daily
execution of project work. These entries are intended to quickly "dump"
decisions, insights, risks, opportunities, and general notes from the
user’s working memory before the end of the day.

Design Intent
-------------
- Acts as a lightweight capture mechanism inside Project Tracker.
- Avoids long-form reflection or journaling within the application.
- Preserves high-value contextual signals that are not represented by
  tasks, time tracking, or status fields.
- Supports later external analysis, summarization, and planning through
  API access and automated reporting processes.

Key Characteristics
-------------------
- One record represents a single narrative event tied to a project.
- Narrative type is application-constrained via `narrative_verb`
  (e.g., NOTE, DECISION, INSIGHT, RISK, OPPORTUNITY).
- Text is intentionally unstructured to encourage fast capture.
- Contact and provider links are optional contextual associations.
- Optimized for chronological queries and future synthesis rather than
  transactional workflow logic.

Out of Scope
------------
- Personal reflection or emotional journaling.
- Daily planning, energy tracking, or freeform narrative summaries.
- Replacement for task comments or meeting minutes.
- Any automated interpretation or scoring of the narrative content.

Future Use
----------
Data in this table is expected to be consumed by external processes that:
- Generate daily or weekly narrative summaries.
- Surface leadership-level signals (decisions, risks, opportunities).
- Provide AI-assisted reflection and forward planning.
- Contribute to long-term institutional or professional memory.

This table is intentionally minimal to maximize capture speed and
long-term schema stability.
*/

CREATE TABLE project_narrative (
    narrative_id        INT AUTO_INCREMENT PRIMARY KEY,
    project_id          INT NOT NULL,
    contact_id          INT NOT NULL,
    provider_id         INT NOT NULL,
    narrative_date      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    narrative_verb      VARCHAR(20) NOT NULL,
    narrative_text      TEXT NOT NULL,

    INDEX idx_project_date (project_id, narrative_date),
    INDEX idx_verb_date (narrative_verb, narrative_date),
    INDEX idx_contact (contact_id),
    INDEX idx_provider (provider_id)
) ENGINE=InnoDB;
