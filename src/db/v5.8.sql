-- web_user and bill_code are legacy MyISAM tables, so MySQL cannot enforce
-- foreign keys to them from this InnoDB table. The indexed reference columns
-- are validated by the application without converting those shared tables.
CREATE TABLE `weekly_report` (
  `weekly_report_id` int NOT NULL AUTO_INCREMENT,
  `report_name` varchar(150) NOT NULL,
  `owner_user_id` int NOT NULL,
  `root_workspace_id` int NOT NULL,
  `root_bill_code` varchar(15) NOT NULL,
  `active` char(1) NOT NULL DEFAULT 'Y',
  `access_key_hash` varchar(64) DEFAULT NULL,
  `access_key_created_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`weekly_report_id`),
  UNIQUE KEY `uk_weekly_report_access_key_hash` (`access_key_hash`),
  KEY `idx_weekly_report_owner` (`owner_user_id`),
  KEY `idx_weekly_report_root_bill_code` (`root_workspace_id`, `root_bill_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `tracker_narrative`
  ADD COLUMN `prompt_used_text` mediumtext NULL AFTER `model_name`;