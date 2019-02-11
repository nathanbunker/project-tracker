CREATE TABLE budget_trans_record (
  trans_record_id INT(11) AUTO_INCREMENT PRIMARY KEY,
  account_id      INT(11) NOT NULL,
  trans_id        INT(11),
  trans_date      DATE NOT NULL,
  trans_amount    INT(11) NOT NULL,
  description     VARCHAR(300) NOT NULL 
);

ALTER TABLE budget_trans ADD COLUMN (trans_record_id INT(11));

ALTER TABLE project ADD COLUMN (iis_region_code VARCHAR(30));