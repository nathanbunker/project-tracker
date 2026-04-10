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

UPDATE project_action_next an
JOIN project p ON p.project_id = an.project_id
SET an.workspace_id = p.workspace_id
WHERE an.workspace_id IS NULL;

UPDATE project_action_taken atk
JOIN project p ON p.project_id = atk.project_id
SET atk.workspace_id = p.workspace_id
WHERE atk.workspace_id IS NULL;

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

-- v5 phase C: editable handles for workspace/project/contact
ALTER TABLE workspace
    ADD COLUMN workspace_handle VARCHAR(60) NULL;

ALTER TABLE project
    ADD COLUMN project_handle VARCHAR(60) NULL;

ALTER TABLE project_contact
    ADD COLUMN contact_handle VARCHAR(60) NULL,
    ADD COLUMN contact_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

UPDATE workspace
SET workspace_handle = LEFT(workspace_name, 60)
WHERE workspace_handle IS NULL OR TRIM(workspace_handle) = '';

UPDATE project
SET project_handle = LEFT(project_name, 60)
WHERE project_handle IS NULL OR TRIM(project_handle) = '';

UPDATE project_contact
SET contact_handle = LEFT(name_first, 60)
WHERE contact_handle IS NULL OR TRIM(contact_handle) = '';

UPDATE project_contact
SET contact_status = 'ACTIVE'
WHERE contact_status IS NULL OR TRIM(contact_status) = '';

ALTER TABLE workspace
    ADD KEY idx_workspace_status_handle (workspace_status, workspace_handle);

ALTER TABLE project
    ADD KEY idx_project_workspace_phase_handle (workspace_id, phase_code, project_handle);

ALTER TABLE project_contact
    ADD KEY idx_project_contact_workspace_status_handle (workspace_id, contact_status, contact_handle);

-- v5 phase D: action sets (Stage 2 refactor)
CREATE TABLE project_action_set (
  action_set_id          INT         NOT NULL AUTO_INCREMENT,
  action_set_type        VARCHAR(16) NOT NULL DEFAULT 'S',
  created_by_web_user_id INT         NOT NULL,
  created_date           DATETIME    NOT NULL,
  PRIMARY KEY (action_set_id),
  KEY idx_pas_web_user (created_by_web_user_id)
);

ALTER TABLE project_action_next
  ADD COLUMN action_set_id INT NULL,
  ADD KEY idx_pan_action_set (action_set_id);

ALTER TABLE project_action_taken
  ADD COLUMN action_set_id INT NULL,
  ADD KEY idx_pat_action_set (action_set_id);

-- v5 phase E: project status + tag model (replace phase/category)
ALTER TABLE project
    ADD COLUMN project_status VARCHAR(20) NULL;

UPDATE project
SET project_status = CASE
    WHEN phase_code = 'Acti' THEN 'Active'
    WHEN phase_code = 'Paus' THEN 'Paused'
    WHEN phase_code = 'Comp' THEN 'Complete'
    WHEN phase_code = 'Clos' THEN 'Closed'
    ELSE 'Active'
END
WHERE project_status IS NULL OR TRIM(project_status) = '';

ALTER TABLE project
    MODIFY COLUMN project_status VARCHAR(20) NOT NULL DEFAULT 'Active';

CREATE TABLE project_tag (
    project_tag_id INT NOT NULL AUTO_INCREMENT,
    workspace_id INT NOT NULL,
    tag_name VARCHAR(100) NOT NULL,
    tag_handle VARCHAR(60) NOT NULL,
    tag_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NULL,
    created_by_web_user_id INT NULL,
    created_date DATETIME NOT NULL,
    PRIMARY KEY (project_tag_id),
    UNIQUE KEY uk_project_tag_workspace_handle (workspace_id, tag_handle),
    KEY idx_project_tag_workspace_status_name (workspace_id, tag_status, tag_name),
    CONSTRAINT fk_project_tag_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(workspace_id),
    CONSTRAINT fk_project_tag_created_by FOREIGN KEY (created_by_web_user_id) REFERENCES web_user(web_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE project_tag_map (
    project_tag_map_id INT NOT NULL AUTO_INCREMENT,
    project_id INT NOT NULL,
    project_tag_id INT NOT NULL,
    created_date DATETIME NOT NULL,
    PRIMARY KEY (project_tag_map_id),
    UNIQUE KEY uk_project_tag_map_project_tag (project_id, project_tag_id),
    KEY idx_project_tag_map_tag_project (project_tag_id, project_id),
    CONSTRAINT fk_project_tag_map_project FOREIGN KEY (project_id) REFERENCES project(project_id),
    CONSTRAINT fk_project_tag_map_tag FOREIGN KEY (project_tag_id) REFERENCES project_tag(project_tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Convert category definitions to workspace tags.
INSERT INTO project_tag (
    workspace_id,
    tag_name,
    tag_handle,
    tag_status,
    sort_order,
    created_by_web_user_id,
    created_date
)
SELECT
    pc.workspace_id,
    LEFT(COALESCE(NULLIF(TRIM(pc.client_name), ''), NULLIF(TRIM(pc.category_code), ''), 'Tag'), 100) AS tag_name,
    LEFT(COALESCE(NULLIF(TRIM(pc.category_code), ''), REPLACE(LOWER(TRIM(pc.client_name)), ' ', '-'), 'tag'), 60) AS tag_handle,
    CASE WHEN UPPER(COALESCE(pc.visible, 'Y')) = 'N' THEN 'INACTIVE' ELSE 'ACTIVE' END AS tag_status,
    pc.sort_order,
    NULL,
    NOW()
FROM project_category pc
WHERE pc.workspace_id IS NOT NULL
    AND NOT EXISTS (
        SELECT 1
        FROM project_tag pt
        WHERE pt.workspace_id = pc.workspace_id
            AND pt.tag_handle = LEFT(COALESCE(NULLIF(TRIM(pc.category_code), ''), REPLACE(LOWER(TRIM(pc.client_name)), ' ', '-'), 'tag'), 60)
    );

-- Create fallback tags for projects whose category code is not present in project_category.
INSERT INTO project_tag (
    workspace_id,
    tag_name,
    tag_handle,
    tag_status,
    sort_order,
    created_by_web_user_id,
    created_date
)
SELECT DISTINCT
    p.workspace_id,
    LEFT(TRIM(p.category_code), 100) AS tag_name,
    LEFT(TRIM(p.category_code), 60) AS tag_handle,
    'ACTIVE',
    NULL,
    NULL,
    NOW()
FROM project p
WHERE p.workspace_id IS NOT NULL
    AND p.category_code IS NOT NULL
    AND TRIM(p.category_code) <> ''
    AND NOT EXISTS (
        SELECT 1
        FROM project_tag pt
        WHERE pt.workspace_id = p.workspace_id
            AND pt.tag_handle = LEFT(TRIM(p.category_code), 60)
    );

-- Convert project.category_code references to project_tag_map.
INSERT INTO project_tag_map (project_id, project_tag_id, created_date)
SELECT
    p.project_id,
    pt.project_tag_id,
    NOW()
FROM project p
JOIN project_tag pt
    ON pt.workspace_id = p.workspace_id
    AND pt.tag_handle = LEFT(TRIM(p.category_code), 60)
WHERE p.category_code IS NOT NULL
    AND TRIM(p.category_code) <> ''
    AND NOT EXISTS (
        SELECT 1
        FROM project_tag_map ptm
        WHERE ptm.project_id = p.project_id
            AND ptm.project_tag_id = pt.project_tag_id
    );

-- Convert patch-link category references to tag references.
ALTER TABLE project_patch_link
    ADD COLUMN linked_patch_tag_id INT NULL;

UPDATE project_patch_link ppl
JOIN project_category pc ON pc.project_category_id = ppl.linked_patch_category_id
JOIN project_tag pt ON pt.workspace_id = pc.workspace_id
    AND pt.tag_handle = LEFT(COALESCE(NULLIF(TRIM(pc.category_code), ''), REPLACE(LOWER(TRIM(pc.client_name)), ' ', '-'), 'tag'), 60)
SET ppl.linked_patch_tag_id = pt.project_tag_id
WHERE ppl.linked_patch_category_id IS NOT NULL;

ALTER TABLE project_patch_link
    ADD CONSTRAINT fk_ppl_patch_tag
        FOREIGN KEY (linked_patch_tag_id) REFERENCES project_tag(project_tag_id);

ALTER TABLE project
    DROP INDEX idx_project_workspace_phase_handle,
    ADD KEY idx_project_workspace_status_handle (workspace_id, project_status, project_handle);

ALTER TABLE project_patch_link
    DROP FOREIGN KEY fk_ppl_patch_category,
    DROP COLUMN linked_patch_category_id;

ALTER TABLE project
    DROP COLUMN phase_code,
    DROP COLUMN provider_name,
    DROP COLUMN profile_id,
    DROP COLUMN category_code;

DROP TABLE project_phase;
DROP TABLE project_category;
-- v5 phase F: remove legacy project_action_ table prefixes
RENAME TABLE
    project_action_next TO action_next,
    project_action_taken TO action_taken,
    project_action_set TO action_set,
    project_action_proposal TO action_proposal,
    project_action_change_log TO action_change_log;

ALTER TABLE action_set RENAME INDEX idx_pas_web_user TO idx_aset_web_user;
ALTER TABLE action_next RENAME INDEX idx_pan_action_set TO idx_an_action_set;
ALTER TABLE action_taken RENAME INDEX idx_pat_action_set TO idx_atk_action_set;