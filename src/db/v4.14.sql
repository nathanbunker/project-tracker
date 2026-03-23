-- Scope bill codes by provider and carry provider_id through associated billing tables.

ALTER TABLE bill_code
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (provider_id, bill_code);

ALTER TABLE bill_budget
  ADD COLUMN provider_id VARCHAR(30) NULL AFTER bill_code;

UPDATE bill_budget bb
JOIN bill_code bc ON bc.bill_code = bb.bill_code
SET bb.provider_id = bc.provider_id
WHERE bb.provider_id IS NULL;

ALTER TABLE bill_budget
  MODIFY COLUMN provider_id VARCHAR(30) NOT NULL,
  DROP INDEX bill_budget_code,
  ADD UNIQUE INDEX uk_bill_budget_provider_code (provider_id, bill_budget_code, bill_code),
  ADD INDEX idx_bill_budget_provider_bill_code (provider_id, bill_code);

ALTER TABLE bill_day
  ADD COLUMN provider_id VARCHAR(30) NULL AFTER bill_code;

UPDATE bill_day bd
JOIN bill_code bc ON bc.bill_code = bd.bill_code
SET bd.provider_id = bc.provider_id
WHERE bd.provider_id IS NULL;

ALTER TABLE bill_day
  MODIFY COLUMN provider_id VARCHAR(30) NOT NULL,
  DROP INDEX bill_code,
  ADD UNIQUE INDEX uk_bill_day_provider_code_date (provider_id, bill_code, bill_date),
  ADD INDEX idx_bill_day_provider_bill_code (provider_id, bill_code);

ALTER TABLE bill_month
  ADD COLUMN provider_id VARCHAR(30) NULL AFTER bill_code;

UPDATE bill_month bm
JOIN bill_code bc ON bc.bill_code = bm.bill_code
SET bm.provider_id = bc.provider_id
WHERE bm.provider_id IS NULL;

ALTER TABLE bill_month
  MODIFY COLUMN provider_id VARCHAR(30) NOT NULL,
  ADD UNIQUE INDEX uk_bill_month_provider_code_date (provider_id, bill_code, bill_date),
  ADD INDEX idx_bill_month_provider_bill_code (provider_id, bill_code);

ALTER TABLE bill_entry
  ADD INDEX idx_bill_entry_provider_bill_code (provider_id, bill_code);

ALTER TABLE project
  ADD INDEX idx_project_provider_bill_code (provider_id, bill_code);

  -- Introduce web_user_id as surrogate primary key on web_user; replace username-based FK linkage.

  ALTER TABLE web_user
    ADD COLUMN web_user_id INT NOT NULL AUTO_INCREMENT FIRST,
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (web_user_id),
    ADD UNIQUE KEY uk_web_user_username (username),
    DROP COLUMN parent_username;

  -- Add account lifecycle/auth fields needed for email-first onboarding.
  ALTER TABLE web_user
    ADD COLUMN first_name VARCHAR(60) NULL AFTER username,
    ADD COLUMN last_name VARCHAR(60) NULL AFTER first_name,
    ADD COLUMN email_address VARCHAR(254) NULL AFTER last_name,
    ADD COLUMN email_verified VARCHAR(1) NOT NULL DEFAULT 'N' AFTER email_address,
    ADD COLUMN registration_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' AFTER user_type,
    ADD COLUMN created_date DATETIME NULL AFTER registration_status,
    ADD COLUMN verified_date DATETIME NULL AFTER created_date,
    ADD COLUMN last_login_date DATETIME NULL AFTER verified_date,
    ADD COLUMN magic_link_token_hash VARCHAR(128) NULL AFTER last_login_date,
    ADD COLUMN magic_link_expiry DATETIME NULL AFTER magic_link_token_hash,
    ADD UNIQUE KEY uk_web_user_email_address (email_address),
    ADD INDEX idx_web_user_registration_status (registration_status),
    ADD INDEX idx_web_user_magic_link_expiry (magic_link_expiry);

  UPDATE web_user wu
    JOIN project_contact pc ON pc.contact_id = wu.contact_id
    SET wu.first_name = pc.name_first,
      wu.last_name = pc.name_last,
      wu.email_address = pc.email_address,
      wu.email_verified = CASE WHEN pc.email_confirmed = 'Y' THEN 'Y' ELSE 'N' END,
      wu.verified_date = CASE WHEN pc.email_confirmed = 'Y' THEN NOW() ELSE NULL END
    WHERE wu.email_address IS NULL;

  UPDATE web_user
    SET created_date = NOW()
    WHERE created_date IS NULL;

  ALTER TABLE web_user
    MODIFY COLUMN created_date DATETIME NOT NULL;

  -- Allow newly registered users to have no provider assigned yet.
  ALTER TABLE web_user
    MODIFY COLUMN provider_id VARCHAR(30) NULL DEFAULT NULL;

  -- bill_entry
  ALTER TABLE bill_entry
    ADD COLUMN web_user_id INT NULL AFTER username;

  UPDATE bill_entry be
    JOIN web_user wu ON wu.username = be.username
    SET be.web_user_id = wu.web_user_id;

  ALTER TABLE bill_entry
    MODIFY COLUMN web_user_id INT NOT NULL,
    ADD INDEX idx_bill_entry_web_user_id (web_user_id);

  -- bill_expected: change composite PK from (username, bill_date) to (web_user_id, bill_date)
  ALTER TABLE bill_expected
    ADD COLUMN web_user_id INT NULL FIRST;

  UPDATE bill_expected be
    JOIN web_user wu ON wu.username = be.username
    SET be.web_user_id = wu.web_user_id;

  ALTER TABLE bill_expected
    DROP PRIMARY KEY,
    MODIFY COLUMN web_user_id INT NOT NULL,
    ADD PRIMARY KEY (web_user_id, bill_date);

  -- project
  ALTER TABLE project
    ADD COLUMN web_user_id INT NULL AFTER username;

  UPDATE project p
    JOIN web_user wu ON wu.username = p.username
    SET p.web_user_id = wu.web_user_id;

  ALTER TABLE project
    MODIFY COLUMN web_user_id INT NOT NULL,
    ADD INDEX idx_project_web_user_id (web_user_id);

  -- project_area
  ALTER TABLE project_area
    ADD COLUMN web_user_id INT NULL AFTER username;

  UPDATE project_area pa
    JOIN web_user wu ON wu.username = pa.username
    SET pa.web_user_id = wu.web_user_id;

  ALTER TABLE project_area
    MODIFY COLUMN web_user_id INT NOT NULL,
    ADD INDEX idx_project_area_web_user_id (web_user_id);

  -- report_profile (nullable: some profiles have no user)
  ALTER TABLE report_profile
    ADD COLUMN web_user_id INT NULL AFTER username;

  UPDATE report_profile rp
    JOIN web_user wu ON wu.username = rp.username
    SET rp.web_user_id = wu.web_user_id
    WHERE rp.username IS NOT NULL;

  ALTER TABLE report_profile
    ADD INDEX idx_report_profile_web_user_id (web_user_id);

  -- web_api_client
  ALTER TABLE web_api_client
    ADD COLUMN web_user_id INT NULL AFTER username;

  UPDATE web_api_client wac
    JOIN web_user wu ON wu.username = wac.username
    SET wac.web_user_id = wu.web_user_id;

  ALTER TABLE web_api_client
    MODIFY COLUMN web_user_id INT NOT NULL,
    ADD INDEX idx_web_api_client_web_user_id (web_user_id);

-- Temporary cleanup: keep only provider 12 and the nbunker_aira web user.
-- WARNING: This permanently deletes historical data outside that scope.
SET @keep_provider_id = '12';
SET @keep_username = 'nbunker_aira';
SET @keep_web_user_id = (
  SELECT wu.web_user_id
  FROM web_user wu
  WHERE wu.username = @keep_username
    AND wu.provider_id = @keep_provider_id
  LIMIT 1
);

-- Safety check (review result before running destructive statements).
SELECT @keep_provider_id AS keep_provider_id, @keep_username AS keep_username,
  @keep_web_user_id AS keep_web_user_id;

-- Re-point retained provider data to the kept web user where applicable.
UPDATE project
SET web_user_id = @keep_web_user_id
WHERE provider_id = @keep_provider_id
  AND web_user_id <> @keep_web_user_id
  AND @keep_web_user_id IS NOT NULL;

UPDATE bill_entry
SET web_user_id = @keep_web_user_id
WHERE provider_id = @keep_provider_id
  AND web_user_id <> @keep_web_user_id
  AND @keep_web_user_id IS NOT NULL;

UPDATE project_area
SET web_user_id = @keep_web_user_id
WHERE web_user_id <> @keep_web_user_id
  AND @keep_web_user_id IS NOT NULL;

UPDATE report_profile
SET web_user_id = @keep_web_user_id
WHERE (provider_id = @keep_provider_id OR provider_id IS NULL)
  AND web_user_id IS NOT NULL
  AND web_user_id <> @keep_web_user_id
  AND @keep_web_user_id IS NOT NULL;

UPDATE web_api_client
SET web_user_id = @keep_web_user_id
WHERE provider_id = @keep_provider_id
  AND web_user_id <> @keep_web_user_id
  AND @keep_web_user_id IS NOT NULL;

-- Remove provider-scoped data outside provider 12.
DELETE FROM bill_budget WHERE provider_id <> @keep_provider_id AND @keep_web_user_id IS NOT NULL;
DELETE FROM bill_day WHERE provider_id <> @keep_provider_id AND @keep_web_user_id IS NOT NULL;
DELETE FROM bill_month WHERE provider_id <> @keep_provider_id AND @keep_web_user_id IS NOT NULL;
DELETE FROM bill_code WHERE provider_id <> @keep_provider_id AND @keep_web_user_id IS NOT NULL;
DELETE FROM bill_entry WHERE provider_id <> @keep_provider_id AND @keep_web_user_id IS NOT NULL;

DELETE pcl
FROM project_action_change_log pcl
JOIN project p ON p.project_id = pcl.project_id
WHERE p.provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE pap
FROM project_action_proposal pap
JOIN project p ON p.project_id = pap.project_id
WHERE p.provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE FROM project_action WHERE provider_id <> @keep_provider_id AND @keep_web_user_id IS NOT NULL;
DELETE FROM project_action_next WHERE provider_id <> @keep_provider_id AND @keep_web_user_id IS NOT NULL;
DELETE FROM project_action_taken WHERE provider_id <> @keep_provider_id AND @keep_web_user_id IS NOT NULL;

DELETE pb
FROM project_bookmark pb
JOIN project p ON p.project_id = pb.project_id
WHERE p.provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE paa
FROM project_area_assigned paa
JOIN project p ON p.project_id = paa.project_id
WHERE p.provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE pca
FROM project_contact_assigned pca
JOIN project p ON p.project_id = pca.project_id
WHERE p.provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE FROM report_profile
WHERE provider_id IS NOT NULL
  AND provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE FROM web_api_client
WHERE provider_id IS NOT NULL
  AND provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE FROM project
WHERE provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE FROM project_category
WHERE provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE ce
FROM contact_event ce
JOIN project_contact pc ON pc.contact_id = ce.contact_id
WHERE pc.provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE pcp
FROM project_contact_position pcp
JOIN project_contact pc ON pc.contact_id = pcp.contact_id
WHERE pc.provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE pcs
FROM project_contact_supervisor pcs
JOIN project_contact pc ON pc.contact_id = pcs.contact_id
WHERE pc.provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE pcs
FROM project_contact_supervisor pcs
JOIN project_contact pc ON pc.contact_id = pcs.supervisor_id
WHERE pc.provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE FROM project_contact
WHERE provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

DELETE FROM project_provider
WHERE provider_id <> @keep_provider_id
  AND @keep_web_user_id IS NOT NULL;

-- Remove user-scoped rows for all users except nbunker_aira.
DELETE FROM bill_expected
WHERE web_user_id <> @keep_web_user_id
  AND @keep_web_user_id IS NOT NULL;

DELETE FROM project_area
WHERE web_user_id <> @keep_web_user_id
  AND @keep_web_user_id IS NOT NULL;

DELETE FROM tracker_keys
WHERE key_type = 2
  AND key_id <> @keep_username
  AND @keep_web_user_id IS NOT NULL;

DELETE FROM web_user
WHERE web_user_id <> @keep_web_user_id
  AND @keep_web_user_id IS NOT NULL;


 update web_user set email_address = "Nathan.Bunker@gmail.com";
 update project_provider set provider_name = 'Nathan Bunker';

-- Track parent/guardian-to-dependent account relationships and invite lifecycle.
CREATE TABLE IF NOT EXISTS we_user_dependency (
  dependency_id INT NOT NULL AUTO_INCREMENT,
  guardian_web_user_id INT NOT NULL,
  dependent_web_user_id INT DEFAULT NULL,
  relationship_type VARCHAR(24) NOT NULL DEFAULT 'guardian',
  dependency_status VARCHAR(16) NOT NULL DEFAULT 'invited',
  can_view_today_summary VARCHAR(1) NOT NULL DEFAULT 'Y',
  can_view_next_actions VARCHAR(1) NOT NULL DEFAULT 'Y',
  can_add_actions VARCHAR(1) NOT NULL DEFAULT 'Y',
  can_edit_actions VARCHAR(1) NOT NULL DEFAULT 'N',
  invite_email VARCHAR(254) DEFAULT NULL,
  invite_token_hash VARCHAR(128) DEFAULT NULL,
  invite_expiry DATETIME DEFAULT NULL,
  created_date DATETIME NOT NULL,
  accepted_date DATETIME DEFAULT NULL,
  ended_date DATETIME DEFAULT NULL,
  PRIMARY KEY (dependency_id),
  INDEX idx_wud_guardian_status (guardian_web_user_id, dependency_status),
  INDEX idx_wud_dependent_status (dependent_web_user_id, dependency_status),
  UNIQUE INDEX uk_wud_guardian_dependent_status (guardian_web_user_id, dependent_web_user_id, dependency_status)
);

ALTER TABLE bill_entry DROP COLUMN username;
ALTER TABLE bill_expected DROP COLUMN username;
ALTER TABLE project DROP COLUMN username;
ALTER TABLE project_area DROP COLUMN username;
ALTER TABLE report_profile DROP COLUMN username;
ALTER TABLE web_api_client DROP COLUMN username;