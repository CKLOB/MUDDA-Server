ALTER TABLE tbl_media ADD COLUMN uploader_id BIGINT;

UPDATE tbl_media AS media
SET uploader_id = capsule.member_id
FROM tbl_time_capsule AS capsule
WHERE media.time_capsule_id = capsule.id;

ALTER TABLE tbl_media ALTER COLUMN uploader_id SET NOT NULL;
ALTER TABLE tbl_media ALTER COLUMN time_capsule_id DROP NOT NULL;
ALTER TABLE tbl_media ALTER COLUMN media_url DROP NOT NULL;

ALTER TABLE tbl_media
    ADD CONSTRAINT fk_media_uploader FOREIGN KEY (uploader_id) REFERENCES tbl_member (id),
    ADD CONSTRAINT uq_media_s3_key UNIQUE (s3_key);

CREATE INDEX idx_media_uploader ON tbl_media (uploader_id);
