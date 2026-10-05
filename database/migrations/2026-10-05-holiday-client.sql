-- Apply once BEFORE deployment, including installations using Hibernate schema updates.
-- Hibernate will not reliably remove the obsolete unique index.
-- Existing holidays remain unassigned; no client IDs are inferred.
ALTER TABLE holidays
    DROP INDEX uk_holiday_location_date,
    ADD COLUMN client_id BIGINT NULL,
    ADD CONSTRAINT fk_holiday_client FOREIGN KEY (client_id) REFERENCES clients(id),
    ADD UNIQUE INDEX uk_holiday_client_location_date (client_id, location_type, holiday_date);
-- The unique index is also the requested client/location/date lookup index.
