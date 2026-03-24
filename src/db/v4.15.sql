-- Add remember-me persistent login support to web_user table.

ALTER TABLE web_user
  ADD COLUMN remember_me_token_hash VARCHAR(128) NULL,
  ADD COLUMN remember_me_expiry     TIMESTAMP    NULL;

-- Add nullable game_points override column to project_action_next.
-- NULL = derive from logic later; 0 = explicit zero; negatives reserved for future use.

ALTER TABLE project_action_next
  ADD COLUMN game_points INT NULL;

-- Immutable ledger recording every point transaction for a contact.

CREATE TABLE game_point_ledger (
  game_point_ledger_id INT           NOT NULL AUTO_INCREMENT,
  contact_id           INT           NOT NULL,
  action_next_id       INT           NULL,
  point_change         INT           NOT NULL,
  entry_type           VARCHAR(20)   NOT NULL,
  entry_note           VARCHAR(1200) NULL,
  created_date         DATETIME      DEFAULT CURRENT_TIMESTAMP,
  created_by           VARCHAR(50)   NULL,
  PRIMARY KEY (game_point_ledger_id),
  INDEX idx_gpl_contact     (contact_id),
  INDEX idx_gpl_action_next (action_next_id),
  INDEX idx_gpl_created     (created_date)
  -- FK constraints omitted: project_contact uses MyISAM and does not support them.
  -- Referential integrity is enforced at the application level.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
