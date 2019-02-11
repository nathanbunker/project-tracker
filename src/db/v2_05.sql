CREATE TABLE bill_month (
  bill_month_id      INT(11) AUTO_INCREMENT PRIMARY KEY,
  bill_code          VARCHAR(15) NOT NULL,
  bill_date          DATE NOT NULL,
  bill_mins_expected INT(11) NOT NULL,
  bill_mins_actual   INT(11) NOT NULL,
  bill_budget_id     INT(11) 
);

ALTER TABLE bill_day ADD COLUMN (bill_month_id INT(11));

ALTER TABLE bill_day ADD COLUMN (bill_mins_budget INT(11) DEFAULT 0);

ALTER TABLE bill_budget ADD COLUMN (bill_mins_remaining INT(11) DEFAULT 0);

