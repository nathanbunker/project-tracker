-- Applies test-safe tracker key overrides after refreshing from production.
-- Key scope: application-level (key_type = 1, key_id = 'APPLICATION').

INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('report.daily.enabled', 1, 'APPLICATION', 'N')
ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('system.email.enable', 1, 'APPLICATION', 'Y')
ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('system.email.smtps.password', 1, 'APPLICATION', '5fa9c8b967f462')
ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('system.email.smtps.port', 1, 'APPLICATION', '587')
ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('system.email.smtps.username', 1, 'APPLICATION', 'd1ab59b8e6b528')
ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('system.email.use.smtps', 1, 'APPLICATION', 'N')
ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('system.smtp.address', 1, 'APPLICATION', 'sandbox.smtp.mailtrap.io')
ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('system.external.url', 1, 'APPLICATION', 'http://localhost:8080/dandelion')
ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

-- Local Windows development: point image uploads to a writable folder.
-- Tomcat runs as NETWORK SERVICE; grant it FullControl on this folder (run once, elevated):
--   $acl = Get-Acl "C:\dev\dandelion\student-offer-images"
--   $rule = New-Object System.Security.AccessControl.FileSystemAccessRule("NETWORK SERVICE","FullControl","ContainerInherit,ObjectInherit","None","Allow")
--   $acl.SetAccessRule($rule); Set-Acl "C:\dev\dandelion\student-offer-images" $acl
INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('student.offer.image.base.folder', 1, 'APPLICATION', 'C:\\dev\\dandelion\\student-offer-images')
ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);

-- Recommended for Mailtrap credentials where username is not an email address:
-- ensure the envelope/header from is valid.
INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('system.email.reply', 1, 'APPLICATION', 'app@dandelion-daily.org')
ON DUPLICATE KEY UPDATE key_value = VALUES(key_value);
