-- v5.2.sql: Add action_next_template_config table and reschedule_locked column

CREATE TABLE `action_next_template_config` (
  `action_next_id` int NOT NULL,
  `auto_generate` char(1) NOT NULL DEFAULT 'Y',
  `missed_action_behavior` varchar(16) NOT NULL DEFAULT 'AUTO_CANCEL',
  `schedule_days_of_week` varchar(30) DEFAULT NULL,
  `schedule_days_of_month` varchar(100) DEFAULT NULL,
  `schedule_days_of_quarter` varchar(200) DEFAULT NULL,
  `schedule_days_of_year` varchar(400) DEFAULT NULL,
  `last_generated_date` date DEFAULT NULL,
  PRIMARY KEY (`action_next_id`),
  KEY `idx_antc_auto_generate` (`auto_generate`),
  CONSTRAINT `fk_antc_action_next` FOREIGN KEY (`action_next_id`) REFERENCES `action_next` (`action_next_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO action_next_template_config (action_next_id, auto_generate, missed_action_behavior)
SELECT action_next_id, 'Y', 'AUTO_CANCEL'
FROM action_next
WHERE template_type IS NOT NULL AND template_type <> '';

ALTER TABLE action_next ADD COLUMN reschedule_locked char(1) NOT NULL DEFAULT 'N' AFTER game_points;

INSERT INTO tracker_keys (key_name, key_type, key_id, key_value)
VALUES ('TEMPLATE_ADVANCE_DAYS', 0, 'global', '14')
ON DUPLICATE KEY UPDATE key_value = key_value;
