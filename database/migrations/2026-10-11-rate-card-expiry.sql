-- Backfill existing rate cards using the same calendar date as the backend.
-- Run in the target database with its session timezone aligned to the backend.
-- End dates are inclusive; cards ending today remain ACTIVE.
-- Safe to rerun. Manually INACTIVE and DRAFT cards are preserved.
START TRANSACTION;

UPDATE rate_cards
SET status = 'EXPIRED'
WHERE UPPER(status) = 'ACTIVE'
  AND effective_to < CURRENT_DATE;

COMMIT;
