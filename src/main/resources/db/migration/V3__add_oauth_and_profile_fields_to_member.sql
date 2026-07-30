ALTER TABLE tbl_member
    ALTER COLUMN name DROP NOT NULL,
    ALTER COLUMN nickname DROP NOT NULL;

-- Member identity is now (oauth_provider, provider_id); email is no longer unique so the same
-- person can sign in with the same email address through different OAuth providers.
ALTER TABLE tbl_member
    DROP CONSTRAINT uq_member_email;

ALTER TABLE tbl_member
    ADD COLUMN oauth_provider VARCHAR(20) NOT NULL,
    ADD COLUMN provider_id VARCHAR(255) NOT NULL,
    ADD COLUMN gender VARCHAR(10),
    ADD COLUMN birth_year INTEGER,
    ADD COLUMN withdrawn_at TIMESTAMP(6);

ALTER TABLE tbl_member
    ADD CONSTRAINT uq_member_oauth_provider_provider_id UNIQUE (oauth_provider, provider_id);
