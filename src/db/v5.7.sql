CREATE TABLE `nathan_access` (
  `nathan_access_id` int NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `created_by_user_id` int DEFAULT NULL,
  `access_type` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `token_hash` varchar(64) DEFAULT NULL,
  `email` varchar(254) DEFAULT NULL,
  `label` varchar(200) DEFAULT NULL,
  `notes` varchar(2000) DEFAULT NULL,
  `requested_at` datetime DEFAULT NULL,
  `approved_at` datetime DEFAULT NULL,
  `approved_by_user_id` int DEFAULT NULL,
  `first_used_at` datetime DEFAULT NULL,
  `last_used_at` datetime DEFAULT NULL,
  `use_count` int NOT NULL DEFAULT '0',
  `expires_at` datetime DEFAULT NULL,
  PRIMARY KEY (`nathan_access_id`),
  UNIQUE KEY `uk_nathan_access_token_hash` (`token_hash`),
  KEY `idx_nathan_access_email` (`email`),
  KEY `idx_nathan_access_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `nathan_access_event` (
  `nathan_access_event_id` int NOT NULL AUTO_INCREMENT,
  `nathan_access_id` int NOT NULL,
  `created_at` datetime NOT NULL,
  `event_type` varchar(30) NOT NULL,
  `content_key` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`nathan_access_event_id`),
  KEY `idx_nathan_access_event_access_created` (`nathan_access_id`,`created_at`),
  KEY `idx_nathan_access_event_type` (`event_type`),
  CONSTRAINT `fk_nathan_access_event_access` FOREIGN KEY (`nathan_access_id`)
    REFERENCES `nathan_access` (`nathan_access_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;