CREATE TABLE `work_obligation` (
  `work_obligation_id` int NOT NULL AUTO_INCREMENT,
  `owner_user_id` int NOT NULL,
  `week_start` date NOT NULL,
  `obligated_minutes` int NOT NULL,
  `note` varchar(150) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`work_obligation_id`),
  UNIQUE KEY `uk_work_obligation_owner_week` (`owner_user_id`, `week_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
