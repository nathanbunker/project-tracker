INSERT INTO report_profile (profile_id, extends_profile_id, profile_label, provider_id, username, profile_type, 
                            use_status, extend_status, definition) VALUES 
                           (3, 0, 'Progress Report', 0, '', 'PRO', 'D', 'E', '');
                           

alter table report_profile modify column profile_id int(11) auto_increment;
