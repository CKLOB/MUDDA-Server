ALTER TABLE tbl_member
    ALTER COLUMN name DROP NOT NULL,
    ALTER COLUMN nickname DROP NOT NULL;

ALTER TABLE tbl_member
    ADD COLUMN oauth_provider VARCHAR(20) NOT NULL,
    ADD COLUMN provider_id VARCHAR(255) NOT NULL,
    ADD COLUMN gender VARCHAR(10),
    ADD COLUMN age INTEGER,
    ADD COLUMN withdrawn_at TIMESTAMP(6);

ALTER TABLE tbl_member
    ADD CONSTRAINT uq_member_oauth_provider_provider_id UNIQUE (oauth_provider, provider_id);
