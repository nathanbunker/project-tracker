-- ---------------------------------------------------------------------
-- AI proposals for projects (may or may not be tied to an existing action)
-- ---------------------------------------------------------------------
CREATE TABLE project_action_proposal (
  proposal_id           INT           NOT NULL AUTO_INCREMENT,
  project_id            INT           NOT NULL,
  action_id             INT           NULL,   -- target project_action.action_id (nullable)
  contact_id            INT           NULL,

  proposal_status       VARCHAR(16)   NOT NULL DEFAULT 'new',  -- new|shown|accepted|rejected|superseded
  proposal_create_date  DATETIME      NOT NULL,
  proposal_decide_date  DATETIME      NULL,

  model_name            VARCHAR(80)   NULL,
  request_id            VARCHAR(120)  NULL,

  proposed_summary      TEXT          NULL,
  proposed_rationale    TEXT          NULL,

  proposed_patch        TEXT          NULL,   -- JSON/text patch of only proposed fields
  input_snapshot        TEXT          NULL,   -- JSON/text context snapshot

  PRIMARY KEY (proposal_id),

  KEY idx_proposal_project_date (project_id, proposal_create_date),
  KEY idx_proposal_project_status (project_id, proposal_status),
  KEY idx_proposal_action_status (action_id, proposal_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ---------------------------------------------------------------------
-- Audit/change log for modifications to project_action (user, AI, system)
-- ---------------------------------------------------------------------
CREATE TABLE project_action_change_log (
  change_id             INT           NOT NULL AUTO_INCREMENT,
  action_id             INT           NOT NULL,
  project_id            INT           NOT NULL,   -- denormalized for fast project-level pull
  proposal_id           INT           NULL,       -- link when change came from accepting a proposal

  change_date           DATETIME      NOT NULL,

  actor_type            VARCHAR(16)   NOT NULL,   -- user|ai|system
  actor_id              VARCHAR(60)   NULL,       -- username/id, provider id, etc.
  source_type           VARCHAR(24)   NULL,       -- ui|api|proposal_accept|bulk_reschedule|etc

  change_summary        TEXT          NULL,       -- human/AI readable line(s)
  change_patch          TEXT          NULL,       -- JSON/text diff: { field: {from,to}, ... }
  change_reason         TEXT          NULL,       -- optional free-text reason

  PRIMARY KEY (change_id),

  KEY idx_change_action_date (action_id, change_date),
  KEY idx_change_project_date (project_id, change_date),
  KEY idx_change_proposal_date (proposal_id, change_date),
  KEY idx_change_actor_date (actor_type, change_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
