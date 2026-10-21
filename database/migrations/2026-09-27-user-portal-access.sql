ALTER TABLE users
    ADD COLUMN portal_access VARCHAR(20) NOT NULL DEFAULT 'FULL',
    ADD CONSTRAINT chk_users_portal_access
        CHECK (portal_access IN ('FULL', 'ONBOARDING', 'BLOCKED'));
