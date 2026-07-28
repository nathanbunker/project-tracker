CREATE TABLE `bill_funding_source` (
  `funding_source_id` int NOT NULL AUTO_INCREMENT,
  `workspace_id` int NOT NULL,
  `funding_source_code` varchar(30) NOT NULL,
  `funding_source_label` varchar(150) NOT NULL,
  `funding_source_type` varchar(20) NOT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `visible` varchar(1) NOT NULL DEFAULT 'Y',
  PRIMARY KEY (`funding_source_id`),
  UNIQUE KEY `uk_bill_funding_source_workspace_code` (`workspace_id`,`funding_source_code`),
  KEY `idx_bill_funding_source_workspace_type` (`workspace_id`,`funding_source_type`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;

ALTER TABLE `bill_code`
  ADD COLUMN `funding_source_id` int DEFAULT NULL AFTER `client_bill_description`,
  ADD KEY `idx_bill_code_funding_source` (`funding_source_id`);

CREATE TABLE `bill_plan` (
  `bill_plan_id` int NOT NULL AUTO_INCREMENT,
  `workspace_id` int NOT NULL,
  `web_user_id` int NOT NULL,
  `bill_plan_code` varchar(30) NOT NULL,
  `plan_label` varchar(150) NOT NULL,
  `fiscal_start_date` date NOT NULL,
  `fiscal_end_date` date NOT NULL,
  `version_num` int NOT NULL,
  `effective_date` date NOT NULL,
  `percent_basis` varchar(30) NOT NULL DEFAULT 'ALL_WORKED_TIME',
  `plan_status` varchar(15) NOT NULL DEFAULT 'DRAFT',
  `supersedes_bill_plan_id` int DEFAULT NULL,
  `change_note` varchar(1000) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`bill_plan_id`),
  UNIQUE KEY `uk_bill_plan_version` (`workspace_id`,`web_user_id`,`bill_plan_code`,`version_num`),
  KEY `idx_bill_plan_effective` (`workspace_id`,`web_user_id`,`effective_date`),
  KEY `idx_bill_plan_status` (`workspace_id`,`web_user_id`,`plan_status`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bill_plan_target` (
  `bill_plan_target_id` int NOT NULL AUTO_INCREMENT,
  `bill_plan_id` int NOT NULL,
  `bill_code` varchar(15) NOT NULL,
  `target_mode` varchar(15) NOT NULL DEFAULT 'PERCENT',
  `annual_target_bps` int DEFAULT NULL,
  `steering_target_bps` int DEFAULT NULL,
  `bill_budget_id` int DEFAULT NULL,
  `variance_policy` varchar(20) NOT NULL DEFAULT 'CARRY_FORWARD',
  `display_order` int NOT NULL DEFAULT '0',
  `target_note` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`bill_plan_target_id`),
  UNIQUE KEY `uk_bill_plan_target` (`bill_plan_id`,`bill_code`),
  KEY `idx_bill_plan_target_code` (`bill_code`),
  KEY `idx_bill_plan_target_budget` (`bill_budget_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4;

ALTER TABLE `bill_entry`
  ADD COLUMN `bill_budget_id` int DEFAULT NULL AFTER `bill_code`,
  ADD KEY `idx_bill_entry_budget` (`bill_budget_id`);

ALTER TABLE `project`
  ADD COLUMN `bill_budget_id` int DEFAULT NULL AFTER `bill_code`,
  ADD KEY `idx_project_bill_budget` (`bill_budget_id`);