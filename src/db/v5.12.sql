ALTER TABLE `project`
  ADD COLUMN `last_modified_date` datetime DEFAULT NULL AFTER `last_modified_by_web_user_id`;
