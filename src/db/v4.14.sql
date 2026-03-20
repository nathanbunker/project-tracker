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
    ADD COLUMN email_address VARCHAR(254) NULL AFTER username,
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
    SET wu.email_address = pc.email_address,
      wu.email_verified = CASE WHEN pc.email_confirmed = 'Y' THEN 'Y' ELSE 'N' END,
      wu.verified_date = CASE WHEN pc.email_confirmed = 'Y' THEN NOW() ELSE NULL END
    WHERE wu.email_address IS NULL;

  UPDATE web_user
    SET created_date = NOW()
    WHERE created_date IS NULL;

  ALTER TABLE web_user
    MODIFY COLUMN created_date DATETIME NOT NULL;

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
