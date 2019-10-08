DROP TABLE sequence;

ALTER TABLE project_contact DROP COLUMN number_cell;
ALTER TABLE project_contact DROP COLUMN number_pager;
ALTER TABLE project_contact DROP COLUMN number_fax;
ALTER TABLE project_contact DROP COLUMN address_street1;
ALTER TABLE project_contact DROP COLUMN address_street2;
ALTER TABLE project_contact DROP COLUMN address_city;
ALTER TABLE project_contact DROP COLUMN address_state;
ALTER TABLE project_contact DROP COLUMN address_zip;
ALTER TABLE project_contact DROP COLUMN address_country;
ALTER TABLE project_contact DROP COLUMN address_lat;
ALTER TABLE project_contact DROP COLUMN address_long;

ALTER TABLE project_contact RENAME COLUMN number_phone TO phone_number;
ALTER TABLE project_contact ADD COLUMN (phone_textable VARCHAR(1) NOT NULL DEFAULT 'N');
ALTER TABLE project_contact RENAME COLUMN email TO email_address;
ALTER TABLE project_contact ADD COLUMN (email_confirmed VARCHAR(1) NOT NULL DEFAULT 'N');
ALTER TABLE project_contact ADD COLUMN (address_id INTEGER);

ALTER TABLE project_client DROP PRIMARY KEY;
ALTER TABLE project_client ADD COLUMN (project_category_id INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY);
CREATE UNIQUE INDEX project_category_code ON project_category (category_code, provider_id);

RENAME TABLE project_client TO project_category; 

ALTER TABLE project_category RENAME COLUMN client_code to category_code;
ALTER TABLE project RENAME COLUMN client_code to category_code;
ALTER TABLE bill_entry RENAME COLUMN client_code to category_code;

CREATE TABLE project_address (
  address_id      INTEGER      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  address_status  VARCHAR(1),
  country         VARCHAR(250),
  line1           VARCHAR(250),
  line2           VARCHAR(250),
  line3           VARCHAR(250),
  line4           VARCHAR(250),
  city            VARCHAR(250),
  state           VARCHAR(250),
  zip             VARCHAR(250),
  time_zone       VARCHAR(250)
);

-- Not run yet

CREATE TABLE tour_offered (
  tour_offered_id INT(11)      AUTO_INCREMENT PRIMARY KEY,
  tour_label      VARCHAR(250) NOT NULL,
  cost_cents
  location
);

CREATE TABLE tour_posted (
  tour_posted_id INT(11)       AUTO_INCREMENT PRIMARY KEY,
  tour_offered_id INT(11)      NOT NULL,
  tour_date_time
  
);

CREATE TABLE tour_booked (
  tour_bunker_id INT(11)       AUTO_INCREMENT PRIMARY KEY,
  
);

CREATE TABLE tour_staff (
  tour_staff_id INT(11)        AUTO_INCREMENT PRIMARY KEY,
  tour_bunker_id 
  contact_id
  driving
  preparing
  guiding
);

CREATE TABLE tour_participant (
  tour_participant_id INT(11)  AUTO_INCREMENT PRIMARY KEY,
  name
  pref_food
);