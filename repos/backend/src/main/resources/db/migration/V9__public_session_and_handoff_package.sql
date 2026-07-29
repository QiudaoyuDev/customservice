ALTER TABLE conversations ADD COLUMN public_access_token_hash varchar(64);
CREATE UNIQUE INDEX ux_conversations_public_access_token_hash ON conversations (public_access_token_hash) WHERE public_access_token_hash IS NOT NULL;

ALTER TABLE handoff_requests ADD COLUMN contact varchar(300);
ALTER TABLE handoff_requests ADD COLUMN package_snapshot text;
