-- v5: workspace ownership foundation
-- Introduces workspace and workspace_member with seeded PRIVATE workspaces.

CREATE TABLE IF NOT EXISTS workspace (
    workspace_id INT NOT NULL,
    workspace_name VARCHAR(100) NOT NULL,
    workspace_type VARCHAR(20) NOT NULL,
    created_by_web_user_id INT NOT NULL,
    workspace_status VARCHAR(20) NOT NULL,
    created_date DATETIME NOT NULL,
    PRIMARY KEY (workspace_id),
    KEY idx_workspace_created_by (created_by_web_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS workspace_member (
    workspace_member_id INT NOT NULL AUTO_INCREMENT,
    workspace_id INT NOT NULL,
    web_user_id INT NOT NULL,
    member_role VARCHAR(20) NOT NULL,
    membership_status VARCHAR(20) NOT NULL,
    created_date DATETIME NOT NULL,
    PRIMARY KEY (workspace_member_id),
    UNIQUE KEY uk_workspace_member_workspace_user (workspace_id, web_user_id),
    UNIQUE KEY uk_workspace_member_user (web_user_id),
    KEY idx_workspace_member_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO workspace (workspace_id, workspace_name, workspace_type, created_by_web_user_id, workspace_status, created_date)
SELECT 1, 'Nathan Private Workspace', 'PRIVATE', 33, 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM workspace WHERE workspace_id = 1);

INSERT INTO workspace (workspace_id, workspace_name, workspace_type, created_by_web_user_id, workspace_status, created_date)
SELECT 2, 'Abbie Private Workspace', 'PRIVATE', 45, 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM workspace WHERE workspace_id = 2);

INSERT INTO workspace_member (workspace_id, web_user_id, member_role, membership_status, created_date)
SELECT 1, 33, 'OWNER', 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM workspace_member WHERE workspace_id = 1 AND web_user_id = 33);

INSERT INTO workspace_member (workspace_id, web_user_id, member_role, membership_status, created_date)
SELECT 2, 45, 'OWNER', 'ACTIVE', NOW()
WHERE NOT EXISTS (SELECT 1 FROM workspace_member WHERE workspace_id = 2 AND web_user_id = 45);

ALTER TABLE project
    ADD COLUMN workspace_id INT NULL,
    ADD COLUMN created_by_web_user_id INT NULL,
    ADD COLUMN last_modified_by_web_user_id INT NULL;

ALTER TABLE project_action_next
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE project_action_taken
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE project_category
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE project_contact
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE report_profile
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE web_api_client
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE bill_code
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE bill_budget
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE bill_day
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE bill_month
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE bill_entry
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE project_action_next
    MODIFY COLUMN provider_id VARCHAR(30) NULL DEFAULT NULL;

ALTER TABLE project_action_taken
    MODIFY COLUMN provider_id VARCHAR(30) NULL DEFAULT NULL;

ALTER TABLE project_category
    MODIFY COLUMN provider_id VARCHAR(30) NULL DEFAULT NULL;

ALTER TABLE project_contact
    MODIFY COLUMN provider_id VARCHAR(30) NULL DEFAULT NULL;

ALTER TABLE bill_entry
    MODIFY COLUMN provider_id VARCHAR(30) NULL DEFAULT NULL;

ALTER TABLE bill_budget
    MODIFY COLUMN provider_id VARCHAR(30) NULL DEFAULT NULL;

ALTER TABLE bill_day
    MODIFY COLUMN provider_id VARCHAR(30) NULL DEFAULT NULL;

ALTER TABLE bill_month
    MODIFY COLUMN provider_id VARCHAR(30) NULL DEFAULT NULL;

UPDATE project
SET workspace_id = CASE web_user_id
    WHEN 33 THEN 1
    WHEN 45 THEN 2
    ELSE workspace_id
END,
created_by_web_user_id = COALESCE(created_by_web_user_id, web_user_id),
last_modified_by_web_user_id = COALESCE(last_modified_by_web_user_id, web_user_id)
WHERE workspace_id IS NULL;

UPDATE project_action_next pan
JOIN project p ON p.project_id = pan.project_id
SET pan.workspace_id = p.workspace_id
WHERE pan.workspace_id IS NULL;

UPDATE project_action_taken pat
JOIN project p ON p.project_id = pat.project_id
SET pat.workspace_id = p.workspace_id
WHERE pat.workspace_id IS NULL;

UPDATE project_category pc
JOIN (
    SELECT provider_id, MAX(workspace_id) AS workspace_id
    FROM project
    WHERE workspace_id IS NOT NULL
    GROUP BY provider_id
) pws ON pws.provider_id = pc.provider_id
SET pc.workspace_id = pws.workspace_id
WHERE pc.workspace_id IS NULL;

UPDATE project_contact pc
JOIN (
    SELECT provider_id, MAX(workspace_id) AS workspace_id
    FROM project
    WHERE workspace_id IS NOT NULL
    GROUP BY provider_id
) pws ON pws.provider_id = pc.provider_id
SET pc.workspace_id = pws.workspace_id
WHERE pc.workspace_id IS NULL;

UPDATE report_profile rp
JOIN workspace_member wm ON wm.web_user_id = rp.web_user_id
SET rp.workspace_id = wm.workspace_id
WHERE rp.workspace_id IS NULL;

UPDATE web_api_client wac
JOIN workspace_member wm ON wm.web_user_id = wac.web_user_id
SET wac.workspace_id = wm.workspace_id
WHERE wac.workspace_id IS NULL;

UPDATE bill_code bc
JOIN project p ON p.provider_id = bc.provider_id AND p.bill_code = bc.bill_code
SET bc.workspace_id = p.workspace_id
WHERE bc.workspace_id IS NULL
    AND p.workspace_id IS NOT NULL;

-- Fallback for bill codes that don't have a direct (provider_id, bill_code) project match.
UPDATE bill_code bc
JOIN (
    SELECT provider_id, MAX(workspace_id) AS workspace_id
    FROM project
    WHERE workspace_id IS NOT NULL
    GROUP BY provider_id
) pws ON pws.provider_id = bc.provider_id
SET bc.workspace_id = pws.workspace_id
WHERE bc.workspace_id IS NULL;

UPDATE bill_budget bb
JOIN bill_code bc ON bc.provider_id = bb.provider_id AND bc.bill_code = bb.bill_code
SET bb.workspace_id = bc.workspace_id
WHERE bb.workspace_id IS NULL
    AND bc.workspace_id IS NOT NULL;

UPDATE bill_day bd
JOIN bill_code bc ON bc.provider_id = bd.provider_id AND bc.bill_code = bd.bill_code
SET bd.workspace_id = bc.workspace_id
WHERE bd.workspace_id IS NULL
    AND bc.workspace_id IS NOT NULL;

UPDATE bill_month bm
JOIN bill_code bc ON bc.provider_id = bm.provider_id AND bc.bill_code = bm.bill_code
SET bm.workspace_id = bc.workspace_id
WHERE bm.workspace_id IS NULL
    AND bc.workspace_id IS NOT NULL;

UPDATE bill_entry be
JOIN project p ON p.project_id = be.project_id
SET be.workspace_id = p.workspace_id
WHERE be.workspace_id IS NULL
    AND p.workspace_id IS NOT NULL;

ALTER TABLE bill_code
    MODIFY COLUMN workspace_id INT NOT NULL;

ALTER TABLE bill_code
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (workspace_id, bill_code);

ALTER TABLE bill_budget
    DROP INDEX uk_bill_budget_provider_code,
    DROP INDEX idx_bill_budget_provider_bill_code,
    ADD UNIQUE KEY uk_bill_budget_workspace_code (workspace_id, bill_budget_code, bill_code),
    ADD KEY idx_bill_budget_workspace_bill_code (workspace_id, bill_code);

ALTER TABLE bill_day
    DROP INDEX uk_bill_day_provider_code_date,
    DROP INDEX idx_bill_day_provider_bill_code,
    ADD UNIQUE KEY uk_bill_day_workspace_code_date (workspace_id, bill_code, bill_date),
    ADD KEY idx_bill_day_workspace_bill_code (workspace_id, bill_code);

ALTER TABLE bill_month
    DROP INDEX uk_bill_month_provider_code_date,
    DROP INDEX idx_bill_month_provider_bill_code,
    ADD UNIQUE KEY uk_bill_month_workspace_code_date (workspace_id, bill_code, bill_date),
    ADD KEY idx_bill_month_workspace_bill_code (workspace_id, bill_code);

ALTER TABLE bill_entry
    DROP INDEX idx_bill_entry_provider_bill_code,
    ADD KEY idx_bill_entry_workspace_bill_code (workspace_id, bill_code);

ALTER TABLE project_narrative
    ADD COLUMN workspace_id INT NULL;

ALTER TABLE project_narrative
    MODIFY COLUMN provider_id VARCHAR(30) NULL DEFAULT NULL;

UPDATE project_narrative pn
JOIN project p ON p.project_id = pn.project_id
SET pn.workspace_id = p.workspace_id
WHERE pn.workspace_id IS NULL
    AND p.workspace_id IS NOT NULL;

ALTER TABLE project_narrative
    DROP INDEX idx_provider,
    ADD KEY idx_project_narrative_workspace_date (workspace_id, narrative_date);

ALTER TABLE workspace
    MODIFY COLUMN workspace_id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE project_action_next
    DROP COLUMN provider_id;

ALTER TABLE project_action_taken
    DROP COLUMN provider_id;

ALTER TABLE project_category
    DROP COLUMN provider_id;

ALTER TABLE project_contact
    DROP COLUMN provider_id;

ALTER TABLE project
    DROP COLUMN provider_id;

ALTER TABLE web_user
    DROP COLUMN provider_id;

ALTER TABLE bill_entry
    DROP COLUMN provider_id;

ALTER TABLE bill_code
    DROP COLUMN provider_id;

ALTER TABLE bill_budget
    DROP COLUMN provider_id;

ALTER TABLE bill_day
    DROP COLUMN provider_id;

ALTER TABLE bill_month
    DROP COLUMN provider_id;

ALTER TABLE project_narrative
    DROP COLUMN provider_id;

ALTER TABLE report_profile
    DROP COLUMN provider_id;

ALTER TABLE web_api_client
    DROP INDEX idx_web_api_client_provider,
    DROP COLUMN provider_id;

DROP TABLE project_provider;

-- v5 phase A: allow users to be members of more than one workspace (personal + patches).
ALTER TABLE workspace_member
    DROP INDEX uk_workspace_member_user;

-- v5 phase B precondition: FK-participating tables must be InnoDB.
-- Legacy environments may still have MyISAM tables, which cannot enforce foreign keys.
ALTER TABLE workspace ENGINE=InnoDB;
ALTER TABLE project ENGINE=InnoDB;
ALTER TABLE project_category ENGINE=InnoDB;
ALTER TABLE web_user ENGINE=InnoDB;

-- v5 phase B: project patch linking
ALTER TABLE project
    ADD COLUMN linked_patch_workspace_id INT NULL,
    ADD CONSTRAINT fk_project_linked_patch_workspace
        FOREIGN KEY (linked_patch_workspace_id) REFERENCES workspace(workspace_id);

CREATE TABLE project_patch_link (
    project_patch_link_id    INT NOT NULL AUTO_INCREMENT,
    private_project_id       INT NOT NULL,
    patch_workspace_id       INT NOT NULL,
    link_type                VARCHAR(20) NOT NULL,
    linked_patch_project_id  INT NULL,
    linked_patch_category_id INT NULL,
    created_by_web_user_id   INT NOT NULL,
    created_date             DATETIME NOT NULL,
    PRIMARY KEY (project_patch_link_id),
    CONSTRAINT fk_ppl_private_project   FOREIGN KEY (private_project_id)       REFERENCES project(project_id),
    CONSTRAINT fk_ppl_patch_workspace   FOREIGN KEY (patch_workspace_id)        REFERENCES workspace(workspace_id),
    CONSTRAINT fk_ppl_patch_project     FOREIGN KEY (linked_patch_project_id)   REFERENCES project(project_id),
    CONSTRAINT fk_ppl_patch_category    FOREIGN KEY (linked_patch_category_id)  REFERENCES project_category(project_category_id),
    CONSTRAINT fk_ppl_created_by        FOREIGN KEY (created_by_web_user_id)    REFERENCES web_user(web_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;