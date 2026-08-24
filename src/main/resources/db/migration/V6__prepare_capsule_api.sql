ALTER TABLE tbl_time_capsule ADD COLUMN open_at TIMESTAMP(6);
UPDATE tbl_time_capsule SET open_at = expires_at WHERE open_at IS NULL;
ALTER TABLE tbl_time_capsule ALTER COLUMN open_at SET NOT NULL;
ALTER TABLE tbl_time_capsule ALTER COLUMN expires_at DROP NOT NULL;

-- Phase 1 of the legacy-column removal: new code no longer maps these columns, while defaults keep
-- the previous blue container able to write during a deployment. Drop them in a later migration.
ALTER TABLE tbl_time_capsule ALTER COLUMN time_capsule_type SET DEFAULT 'DEFAULT';
ALTER TABLE tbl_time_capsule ALTER COLUMN is_anonymous SET DEFAULT FALSE;
ALTER TABLE tbl_time_capsule ALTER COLUMN is_feed_public SET DEFAULT FALSE;

ALTER TABLE tbl_guestbook ADD COLUMN updated_at TIMESTAMP(6);
UPDATE tbl_guestbook SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE tbl_guestbook ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX idx_time_capsule_member_deleted_created
    ON tbl_time_capsule (member_id, is_deleted, created_at DESC);
CREATE INDEX idx_capsule_recipient_member_created
    ON tbl_capsule_recipient (member_id, created_at DESC);
