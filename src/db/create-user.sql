CREATE USER 'tracker_web'@'localhost' IDENTIFIED BY 'SharkBaitHooHaHa';

GRANT ALL PRIVILEGES ON tracker.* TO 'tracker_web'@'localhost' WITH GRANT OPTION;