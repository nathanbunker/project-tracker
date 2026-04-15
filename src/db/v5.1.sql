-- Migration: move project_action_next.next_notes into action_next_note rows

CREATE TABLE `action_next_note` (
  `action_next_note_id` int NOT NULL AUTO_INCREMENT,
  `action_next_id` int NOT NULL,
  `contact_id` int NOT NULL,
  `note_line` text NOT NULL,
  `note_date` datetime NOT NULL,
  PRIMARY KEY (`action_next_note_id`),
  KEY `idx_ann_action` (`action_next_id`),
  KEY `idx_ann_contact` (`contact_id`),
  KEY `idx_ann_note_date` (`note_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO action_next_note (action_next_id, contact_id, note_line, note_date)
SELECT action_next_id, contact_id, TRIM(next_notes), COALESCE(next_change_date, NOW())
FROM action_next
WHERE next_notes IS NOT NULL AND TRIM(next_notes) <> '';

ALTER TABLE action_next DROP COLUMN next_notes;
