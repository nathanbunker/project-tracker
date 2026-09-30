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
