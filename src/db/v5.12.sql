-- Linked calendar occurrences for MCP; apply manually after v5.11.sql.
-- Epoch milliseconds avoid dependence on JDBC/server/session time zones.
CREATE TABLE `linked_meeting` (
  `linked_meeting_id` int NOT NULL AUTO_INCREMENT,
  `version` bigint NOT NULL DEFAULT 0,
  `workspace_id` int NOT NULL,
  `action_next_id` int NOT NULL,
  `source_system` varchar(64) COLLATE utf8mb4_0900_bin NOT NULL,
  `source_namespace` varchar(128) COLLATE utf8mb4_0900_bin NOT NULL,
  `external_event_uid` varchar(255) COLLATE utf8mb4_0900_bin NOT NULL,
  `external_occurrence_id` varchar(128) COLLATE utf8mb4_0900_bin NOT NULL,
  `original_subject` varchar(512) NOT NULL,
  `original_description` mediumtext,
  `organizer_name` varchar(255) DEFAULT NULL,
  `organizer_address` varchar(320) DEFAULT NULL,
  `scheduled_start_epoch_ms` bigint NOT NULL,
  `scheduled_end_epoch_ms` bigint NOT NULL,
  `source_time_zone` varchar(128) DEFAULT NULL,
  `location` varchar(2048) DEFAULT NULL,
  `join_url` varchar(1200) DEFAULT NULL,
  `source_status` varchar(16) NOT NULL,
  `source_last_modified_epoch_ms` bigint DEFAULT NULL,
  `source_revision` varchar(255) DEFAULT NULL,
  `source_fingerprint` varchar(255) DEFAULT NULL,
  `last_synced_epoch_ms` bigint NOT NULL,
  PRIMARY KEY (`linked_meeting_id`),
  UNIQUE KEY `uk_linked_meeting_action` (`action_next_id`),
  UNIQUE KEY `uk_linked_meeting_occurrence` (`workspace_id`, `source_system`, `source_namespace`,
    `external_event_uid`, `external_occurrence_id`),
  KEY `idx_linked_meeting_schedule` (`workspace_id`, `scheduled_start_epoch_ms`),
  CONSTRAINT `fk_linked_meeting_action` FOREIGN KEY (`action_next_id`)
    REFERENCES `action_next` (`action_next_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
