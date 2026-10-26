-- Retire unused Dandelion meeting agenda/note persistence from v5.3.
-- Apply manually with the release that removes the corresponding Hibernate mappings.
-- Project/contact external synchronization fields and name_title are unchanged.

DROP TABLE `meeting_note_line`;
DROP TABLE `meeting_session_topic`;
DROP TABLE `meeting_session`;
