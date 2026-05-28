-- v5.3.sql: Add Dandelion meeting session/topic/note persistence tables

CREATE TABLE `meeting_session` (
  `meeting_session_id` int NOT NULL AUTO_INCREMENT,
  `workspace_id` int NOT NULL,
  `interop_hub_meeting_id` int DEFAULT NULL,
  `meeting_key` varchar(80) DEFAULT NULL,
  `meeting_name` varchar(160) NOT NULL,
  `meeting_description` text,
  `scheduled_start` datetime NOT NULL,
  `scheduled_end` datetime DEFAULT NULL,
  `timezone_id` varchar(64) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `parent_project_id` int DEFAULT NULL,
  `online_meeting_url` varchar(2048) DEFAULT NULL,
  `online_meeting_details` text,
  `cancellation_reason` text,
  `interop_hub_updated_at` datetime DEFAULT NULL,
  `last_synced_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by_user_id` int DEFAULT NULL,
  `updated_by_user_id` int DEFAULT NULL,
  PRIMARY KEY (`meeting_session_id`),
  KEY `idx_meeting_session_workspace_id` (`workspace_id`),
  KEY `idx_meeting_session_interop_hub_meeting_id` (`interop_hub_meeting_id`),
  KEY `idx_meeting_session_meeting_key` (`meeting_key`),
  KEY `idx_meeting_session_scheduled_start` (`scheduled_start`),
  KEY `idx_meeting_session_parent_project_id` (`parent_project_id`),
  KEY `idx_meeting_session_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `meeting_session_topic` (
  `meeting_session_topic_id` int NOT NULL AUTO_INCREMENT,
  `meeting_session_id` int NOT NULL,
  `workspace_id` int NOT NULL,
  `interop_hub_agenda_item_id` int DEFAULT NULL,
  `interop_hub_topic_id` int DEFAULT NULL,
  `project_id` int DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `agenda_markdown` text,
  `display_order` int NOT NULL,
  `time_minutes` int DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `status_note` text,
  `postponed_to_interop_hub_meeting_id` int DEFAULT NULL,
  `interop_hub_updated_at` datetime DEFAULT NULL,
  `last_synced_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`meeting_session_topic_id`),
  KEY `idx_meeting_session_topic_meeting_session_id` (`meeting_session_id`),
  KEY `idx_meeting_session_topic_workspace_id` (`workspace_id`),
  KEY `idx_meeting_session_topic_interop_hub_agenda_item_id` (`interop_hub_agenda_item_id`),
  KEY `idx_meeting_session_topic_interop_hub_topic_id` (`interop_hub_topic_id`),
  KEY `idx_meeting_session_topic_project_id` (`project_id`),
  KEY `idx_meeting_session_topic_display_order` (`display_order`),
  KEY `idx_meeting_session_topic_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `meeting_note_line` (
  `meeting_note_line_id` int NOT NULL AUTO_INCREMENT,
  `meeting_session_topic_id` int NOT NULL,
  `workspace_id` int NOT NULL,
  `parent_meeting_note_line_id` int DEFAULT NULL,
  `display_order` int NOT NULL,
  `note_text` text NOT NULL,
  `narrative_category` varchar(20) DEFAULT NULL,
  `action_type` varchar(32) DEFAULT NULL,
  `linked_next_action_id` int DEFAULT NULL,
  `visibility` varchar(20) NOT NULL DEFAULT 'INTERNAL',
  `url_detected` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `created_by_user_id` int DEFAULT NULL,
  `updated_by_user_id` int DEFAULT NULL,
  PRIMARY KEY (`meeting_note_line_id`),
  KEY `idx_meeting_note_line_meeting_session_topic_id` (`meeting_session_topic_id`),
  KEY `idx_meeting_note_line_workspace_id` (`workspace_id`),
  KEY `idx_meeting_note_line_parent_meeting_note_line_id` (`parent_meeting_note_line_id`),
  KEY `idx_meeting_note_line_display_order` (`display_order`),
  KEY `idx_meeting_note_line_narrative_category` (`narrative_category`),
  KEY `idx_meeting_note_line_action_type` (`action_type`),
  KEY `idx_meeting_note_line_linked_next_action_id` (`linked_next_action_id`),
  KEY `idx_meeting_note_line_visibility` (`visibility`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- v5.3 addition: external sync identity for project + project_contact
ALTER TABLE project_contact
  MODIFY COLUMN name_title varchar(200) DEFAULT NULL;

ALTER TABLE project
  ADD COLUMN external_source_key varchar(100) DEFAULT NULL,
  ADD COLUMN external_project_id varchar(120) DEFAULT NULL,
  ADD COLUMN external_managed char(1) NOT NULL DEFAULT 'N',
  ADD COLUMN external_last_synced_at datetime DEFAULT NULL;

ALTER TABLE project
  ADD KEY idx_project_workspace_external (workspace_id, external_source_key, external_project_id),
  ADD UNIQUE KEY uk_project_workspace_external (workspace_id, external_source_key, external_project_id);

ALTER TABLE project_contact
  ADD COLUMN external_source_key varchar(100) DEFAULT NULL,
  ADD COLUMN external_contact_id varchar(120) DEFAULT NULL,
  ADD COLUMN external_managed char(1) NOT NULL DEFAULT 'N',
  ADD COLUMN external_last_synced_at datetime DEFAULT NULL;

ALTER TABLE project_contact
  ADD KEY idx_project_contact_workspace_external (workspace_id, external_source_key, external_contact_id),
  ADD UNIQUE KEY uk_project_contact_workspace_external (workspace_id, external_source_key, external_contact_id);
