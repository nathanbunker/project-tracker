/*
Add project_icon to support storing pasted emoji directly.

Note:
- This column uses utf8mb4 so emoji values can be stored.
- Ensure the project table/database charset/collation is utf8mb4-compatible.
*/

ALTER TABLE project
    ADD COLUMN project_icon VARCHAR(8)
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci
    NULL;

-- Set connection charset for good measure. These updates below intentionally use
-- utf8mb4 byte literals so they still work even if the SQL client terminal is
-- not configured to input/display emoji correctly.
SET NAMES utf8mb4;

-- Assign emojis to personal projects (safe from client encoding issues)

UPDATE project SET project_icon = CONVERT(0xF09F9A97 USING utf8mb4) WHERE project_id = 48711; -- Cars (🚗)
UPDATE project SET project_icon = CONVERT(0xF09F8EB5 USING utf8mb4) WHERE project_id = 48714; -- Choir (🎵)
UPDATE project SET project_icon = CONVERT(0xF09F91A8E2808DF09F91A9E2808DF09F91A7E2808DF09F91A6 USING utf8mb4) WHERE project_id = 48719; -- Family and Friends (👨‍👩‍👧‍👦)
UPDATE project SET project_icon = CONVERT(0xF09F92B0 USING utf8mb4) WHERE project_id = 48709; -- Finances (💰)
UPDATE project SET project_icon = CONVERT(0xF09F87ABF09F87B7 USING utf8mb4) WHERE project_id = 48718; -- French (🇫🇷)
UPDATE project SET project_icon = CONVERT(0xF09F92AA USING utf8mb4) WHERE project_id = 48713; -- Health (💪)
UPDATE project SET project_icon = CONVERT(0xF09F8FA0 USING utf8mb4) WHERE project_id = 48710; -- House (🏠)
UPDATE project SET project_icon = CONVERT(0xE29BAA USING utf8mb4) WHERE project_id = 48716; -- HVC (⛪)
UPDATE project SET project_icon = CONVERT(0xE29C89EFB88F USING utf8mb4) WHERE project_id = 48717; -- Letters (✉️)
UPDATE project SET project_icon = CONVERT(0xF09FA49D USING utf8mb4) WHERE project_id = 48715; -- Ministering (🤝)
UPDATE project SET project_icon = CONVERT(0xF09F8CB2 USING utf8mb4) WHERE project_id = 48712; -- Outdoors (🌲)