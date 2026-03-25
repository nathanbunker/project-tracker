-- Add remember-me persistent login support to web_user table.

ALTER TABLE web_user
  ADD COLUMN remember_me_token_hash VARCHAR(128) NULL,
  ADD COLUMN remember_me_expiry     TIMESTAMP    NULL;

ALTER TABLE web_user
  ADD COLUMN workflow_type ENUM('STANDARD', 'STUDENT') NOT NULL DEFAULT 'STANDARD'
  AFTER user_type;

-- Add nullable game_points override column to project_action_next.
-- NULL = derive from logic later; 0 = explicit zero; negatives reserved for future use.

ALTER TABLE project_action_next
  ADD COLUMN game_points INT NULL;

-- Parent-managed offer template catalog.

CREATE TABLE student_offer_template (
  student_offer_template_id INT          NOT NULL AUTO_INCREMENT,
  contact_id                INT          NOT NULL,
  title                     VARCHAR(200) NOT NULL,
  description               TEXT         NULL,
  default_price_points      INT          NOT NULL,
  image_path                VARCHAR(500) NULL,
  status                    VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  display_order             INT          NOT NULL DEFAULT 0,
  created_date              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_date              DATETIME     NULL,
  PRIMARY KEY (student_offer_template_id),
  INDEX idx_sot_contact      (contact_id),
  INDEX idx_sot_status       (status),
  INDEX idx_sot_display      (display_order)
  -- FK to project_contact omitted because legacy project_contact is MyISAM.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Child/dependent offer copy shown in a student's store.

CREATE TABLE student_offer (
  student_offer_id          INT          NOT NULL AUTO_INCREMENT,
  contact_id                INT          NOT NULL,
  student_offer_template_id INT          NULL,
  title                     VARCHAR(200) NOT NULL,
  description               TEXT         NULL,
  price_points              INT          NOT NULL,
  image_path                VARCHAR(500) NULL,
  status                    VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
  display_order             INT          NOT NULL DEFAULT 0,
  created_date              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_date              DATETIME     NULL,
  bought_date               DATETIME     NULL,
  fulfilling_date           DATETIME     NULL,
  delivered_date            DATETIME     NULL,
  PRIMARY KEY (student_offer_id),
  INDEX idx_so_contact       (contact_id),
  INDEX idx_so_template      (student_offer_template_id),
  INDEX idx_so_status        (status),
  INDEX idx_so_display       (display_order),
  CONSTRAINT fk_so_template FOREIGN KEY (student_offer_template_id)
    REFERENCES student_offer_template (student_offer_template_id)
  -- FK to project_contact omitted because legacy project_contact is MyISAM.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Immutable ledger recording every point transaction for a contact.

CREATE TABLE game_point_ledger (
  game_point_ledger_id INT           NOT NULL AUTO_INCREMENT,
  contact_id           INT           NOT NULL,
  action_next_id       INT           NULL,
  student_offer_id     INT           NULL,
  point_change         INT           NOT NULL,
  entry_type           VARCHAR(20)   NOT NULL,
  entry_note           VARCHAR(1200) NULL,
  created_date         DATETIME      DEFAULT CURRENT_TIMESTAMP,
  created_by           VARCHAR(50)   NULL,
  PRIMARY KEY (game_point_ledger_id),
  INDEX idx_gpl_contact     (contact_id),
  INDEX idx_gpl_action_next (action_next_id),
  INDEX idx_gpl_offer       (student_offer_id),
  INDEX idx_gpl_created     (created_date),
  CONSTRAINT fk_gpl_offer FOREIGN KEY (student_offer_id)
    REFERENCES student_offer (student_offer_id)
  -- FK constraints omitted: project_contact uses MyISAM and does not support them.
  -- Referential integrity is enforced at the application level.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
