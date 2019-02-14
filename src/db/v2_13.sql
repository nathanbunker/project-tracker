CREATE TABLE project_contact_supervisor (
  contact_supervisor_id INT(11) AUTO_INCREMENT PRIMARY KEY,
  contact_id            INT(11) NOT NULL,
  supervisor_id         INT(11) NOT NULL,
  email_alert           VARCHAR(1) DEFAULT 'N'
);