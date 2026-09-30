CREATE TABLE `planning_outlook` (
  `outlook_id` int NOT NULL AUTO_INCREMENT,
  `owner_user_id` int NOT NULL,
  `period_type` varchar(10) NOT NULL,
  `period_start` date NOT NULL,
  `outlook_text` text,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`outlook_id`),
  UNIQUE KEY `uk_planning_outlook_owner_period` (`owner_user_id`, `period_type`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `project_ai_note` (
  `note_id` int NOT NULL AUTO_INCREMENT,
  `project_id` int NOT NULL,
  `note_text` text NOT NULL,
  `source` varchar(80) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`note_id`),
  KEY `idx_project_ai_note_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `project`
  ADD COLUMN `last_modified_date` datetime DEFAULT NULL AFTER `last_modified_by_web_user_id`;
