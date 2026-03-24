-- Add remember-me persistent login support to web_user table.

ALTER TABLE web_user
  ADD COLUMN remember_me_token_hash VARCHAR(128) NULL,
  ADD COLUMN remember_me_expiry     TIMESTAMP    NULL;
