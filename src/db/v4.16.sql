-- v4.16: Multi-device remember-me support
-- Replaces the single per-user token columns with a dedicated token table
-- so that logging in on one device no longer invalidates other devices.

CREATE TABLE remember_me_token (
    token_id    INT          NOT NULL AUTO_INCREMENT,
    web_user_id INT          NOT NULL,
    token_hash  VARCHAR(128) NOT NULL,
    expiry      DATETIME     NOT NULL,
    PRIMARY KEY (token_id),
    INDEX idx_rmt_user (web_user_id),
    INDEX idx_rmt_hash (token_hash)
);

ALTER TABLE web_user
    DROP COLUMN remember_me_token_hash,
    DROP COLUMN remember_me_expiry;
