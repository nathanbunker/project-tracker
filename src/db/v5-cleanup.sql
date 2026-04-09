-- v5 cleanup: remove closed projects and directly related records
-- NOTE: This script currently targets phase_code = 'Acti' per request.
-- If your closed phase code is different, update the WHERE clause below.

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_projects_to_delete (
    project_id INT NOT NULL PRIMARY KEY
);

INSERT INTO tmp_projects_to_delete (project_id)
SELECT p.project_id
FROM project p
WHERE p.phase_code = 'Clos';

-- Preview the number of projects selected for deletion.
SELECT COUNT(*) AS projects_to_delete
FROM tmp_projects_to_delete;

-- Delete child/dependent rows first.
DELETE pacl
FROM project_action_change_log pacl
JOIN tmp_projects_to_delete tpd ON tpd.project_id = pacl.project_id;

DELETE pan
FROM project_action_next pan
JOIN tmp_projects_to_delete tpd ON tpd.project_id = pan.project_id;

DELETE pap
FROM project_action_proposal pap
JOIN tmp_projects_to_delete tpd ON tpd.project_id = pap.project_id;

DELETE pat
FROM project_action_taken pat
JOIN tmp_projects_to_delete tpd ON tpd.project_id = pat.project_id;

DELETE paa
FROM project_area_assigned paa
JOIN tmp_projects_to_delete tpd ON tpd.project_id = paa.project_id;

DELETE pb
FROM project_bookmark pb
JOIN tmp_projects_to_delete tpd ON tpd.project_id = pb.project_id;

DELETE pca
FROM project_contact_assigned pca
JOIN tmp_projects_to_delete tpd ON tpd.project_id = pca.project_id;

DELETE pi
FROM project_issue pi
JOIN tmp_projects_to_delete tpd ON tpd.project_id = pi.project_id;

DELETE pn
FROM project_narrative pn
JOIN tmp_projects_to_delete tpd ON tpd.project_id = pn.project_id;

DELETE tn
FROM tracker_narrative tn
JOIN tmp_projects_to_delete tpd ON tpd.project_id = tn.project_id;

DELETE be
FROM bill_entry be
JOIN tmp_projects_to_delete tpd ON tpd.project_id = be.project_id;

-- v5 table: clean rows that reference deleted projects in either project link column.
-- Execute conditionally so the script also works before this table exists.
SET @has_project_patch_link := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'project_patch_link'
);

SET @sql_cleanup_project_patch_link := IF(
    @has_project_patch_link > 0,
    'DELETE ppl FROM project_patch_link ppl JOIN tmp_projects_to_delete tpd ON tpd.project_id = ppl.private_project_id OR tpd.project_id = ppl.linked_patch_project_id',
    'SELECT 1'
);

PREPARE stmt_cleanup_project_patch_link FROM @sql_cleanup_project_patch_link;
EXECUTE stmt_cleanup_project_patch_link;
DEALLOCATE PREPARE stmt_cleanup_project_patch_link;

-- Delete parent rows last.
DELETE p
FROM project p
JOIN tmp_projects_to_delete tpd ON tpd.project_id = p.project_id;

DROP TEMPORARY TABLE tmp_projects_to_delete;

COMMIT;

DELETE FROM project_category pc
WHERE NOT EXISTS (
    SELECT 1
    FROM project p
    WHERE p.category_code = pc.category_code
);