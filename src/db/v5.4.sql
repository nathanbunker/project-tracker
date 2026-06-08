ALTER TABLE action_next MODIFY contact_id INT NULL;

ALTER TABLE project
  ADD COLUMN current_focus_text TEXT NULL;

CREATE TABLE `project_fact_definition` (
  `project_fact_definition_id` int NOT NULL AUTO_INCREMENT,
  `workspace_id` int NOT NULL,
  `fact_group` varchar(60) NOT NULL,
  `fact_code` varchar(80) NOT NULL,
  `fact_label` varchar(200) NOT NULL,
  `fact_description` varchar(1200) DEFAULT NULL,
  `fact_input_type` varchar(20) NOT NULL DEFAULT 'BOOLEAN',
  `display_order` int NOT NULL DEFAULT '0',
  `active` char(1) NOT NULL DEFAULT 'Y',
  `created_by_web_user_id` int DEFAULT NULL,
  `created_date` datetime NOT NULL,
  `last_modified_by_web_user_id` int DEFAULT NULL,
  `last_modified_date` datetime DEFAULT NULL,
  PRIMARY KEY (`project_fact_definition_id`),
  KEY `idx_project_fact_definition_workspace_id` (`workspace_id`),
  KEY `idx_project_fact_definition_workspace_group` (`workspace_id`,`fact_group`),
  UNIQUE KEY `uk_project_fact_definition_workspace_code` (`workspace_id`,`fact_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `project_fact_value` (
  `project_fact_value_id` int NOT NULL AUTO_INCREMENT,
  `project_id` int NOT NULL,
  `project_fact_definition_id` int NOT NULL,
  `value_boolean` char(1) DEFAULT NULL,
  `value_text` varchar(1200) DEFAULT NULL,
  `value_date` date DEFAULT NULL,
  `value_number` decimal(18,4) DEFAULT NULL,
  `value_code` varchar(80) DEFAULT NULL,
  `notes` varchar(1200) DEFAULT NULL,
  `created_by_web_user_id` int DEFAULT NULL,
  `created_date` datetime NOT NULL,
  `last_modified_by_web_user_id` int DEFAULT NULL,
  `last_modified_date` datetime DEFAULT NULL,
  PRIMARY KEY (`project_fact_value_id`),
  UNIQUE KEY `uk_project_fact_value_project_definition` (`project_id`,`project_fact_definition_id`),
  KEY `idx_project_fact_value_project_id` (`project_id`),
  KEY `idx_project_fact_value_definition_id` (`project_fact_definition_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
