alter table project_action modify column action_id int(11) auto_increment;
alter table project modify column project_id int(11) auto_increment;
alter table project_contact modify column contact_id int(11) auto_increment;
alter table bill_entry modify column bill_id int(11) auto_increment;

alter table project add column (iis_submission_code varchar(30));
alter table project add column (iis_facility_id varchar(30));
alter table project add column (medical_organization varchar(60));

alter table bill_code add column (estimate_min  int(11) not null default 0);
alter table bill_code add column (bill_rate int(11) not null default 0);
alter table bill_code add column (bill_round int(11) not null default 1);

alter table web_user add column (user_type varchar(30) not null default 'user');
update web_user set user_type = 'admin' where username = 'nbunker';
update web_user set user_type = 'admin' where username = 'nbunker_mcir';
update web_user set user_type = 'admin' where username = 'nathan_ng';

CREATE TABLE bill_day (
  bill_day_id INT(11) AUTO_INCREMENT PRIMARY KEY,
  bill_code   VARCHAR(15) NOT NULL,
  bill_date   DATE NOT NULL,
  bill_mins   INT(11) NOT NULL,
  bill_budget_id  INT(11) 
);

ALTER TABLE bill_day ADD UNIQUE INDEX (bill_code, bill_date);

CREATE TABLE bill_budget (
  bill_budget_id   INT(11) AUTO_INCREMENT PRIMARY KEY,
  bill_budget_code VARCHAR(30) NOT NULL,
  bill_code        VARCHAR(15) NOT NULL,
  start_date       DATE NOT NULL,
  end_date         DATE NOT NULL,
  bill_mins        INT(11) NOT NULL
);

ALTER TABLE bill_budget ADD UNIQUE INDEX (bill_budget_code, bill_code);

CREATE TABLE budget_account (
  account_id      INT(11) AUTO_INCREMENT PRIMARY KEY,
  account_label   VARCHAR(30) NOT NULL,
  provider_id     VARCHAR(30) NOT NULL,
  start_amount    INT(11) NOT NULL,
  start_date      DATE NOT NULL,
  balance_amount  INT(11) NOT NULL,
  balance_date    DATE NOT NULL
);

ALTER TABLE budget_account ADD UNIQUE INDEX (account_label, provider_id);

CREATE TABLE budget_month (
  month_id        INT(11) AUTO_INCREMENT PRIMARY KEY,
  month_date      DATE NOT NULL,
  account_id      INT(11) NOT NULL,
  balance_start   INT(11) NOT NULL,
  balance_end     INT(11) NOT NULL
);

ALTER TABLE budget_month ADD UNIQUE INDEX (month_date, account_id);

CREATE TABLE budget_item
(
  item_id          INT(11) AUTO_INCREMENT PRIMARY KEY,
  item_label       VARCHAR(30) NOT NULL,
  account_id       INT(11) NOT NULL,  
  item_status      VARCHAR(1) NOT NULL,
     -- M Monthly
     -- Y Yearly
     -- S Sporadic
     -- O One time
     -- C Closed
  last_amount     INT(11) NOT NULL,
  last_date       DATE NOT NULL,
  priority_code   VARCHAR(1) NOT NULL,
     -- A Committed
     -- B Scheduled
     -- C Flexible
     -- P Debt Payoff
     -- S Savings
  related_item_id INT(11) 
);

ALTER TABLE budget_item ADD UNIQUE INDEX (item_label, account_id);

CREATE TABLE budget_trans 
(
  trans_id         INT(11) AUTO_INCREMENT PRIMARY KEY,
  item_id          INT(11) NOT NULL,
  month_id         INT(11) NOT NULL,
  trans_date       DATE NOT NULL,
  trans_status     VARCHAR(1) NOT NULL,
    -- E Expected
    -- B Possible
    -- S Scheduled
    -- P Pending
    -- D Due
    -- P Paid
  trans_amount    INT(11) NOT NULL,
  related_trans_id INT(11)
);

ALTER TABLE budget_trans ADD UNIQUE INDEX (item_id, month_id);

INSERT INTO tracker_keys (key_name, key_type, key_id, key_value) VALUES ('manage.budget', 2, 'nbunker', 'Y');

ALTER TABLE web_user ADD COLUMN (parent_username VARCHAR(30));

UPDATE web_user SET parent_username = 'nbunker' where username = 'nbunker_mcir';
UPDATE web_user SET parent_username = 'nbunker' where username = 'nbunker_tch';
UPDATE project_provider SET provider_name = 'DSR' where provider_id = 1;

CREATE TABLE contact_event
(
  event_id      INT(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  contact_id    INT(11) NOT NULL,
  event_type    VARCHAR(1) NOT NULL,
    -- B = Birth Date
    -- W = Wedding anniversary
    -- M = Met anniversary
  event_num     INT(11) NOT NULL,
  event_date    DATE NOT NULL
);

