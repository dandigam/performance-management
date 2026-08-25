RENAME TABLE sow_resource_allocations TO sow_milestone_positions;

ALTER TABLE sow_milestone_positions
    RENAME INDEX idx_sow_resource_allocation_sow_id
        TO idx_sow_milestone_position_sow_id,
    RENAME INDEX idx_sow_resource_allocation_milestone_id
        TO idx_sow_milestone_position_milestone_id,
    RENAME INDEX idx_sow_resource_allocation_position_id
        TO idx_sow_milestone_position_position_id,
    RENAME INDEX idx_sow_resource_allocation_rate_card_id
        TO idx_sow_milestone_position_rate_card_id,
    DROP FOREIGN KEY fk_sow_resource_allocation_sow,
    DROP FOREIGN KEY fk_sow_resource_allocation_milestone,
    DROP FOREIGN KEY fk_sow_resource_allocation_position,
    DROP FOREIGN KEY fk_sow_resource_allocation_rate_card,
    ADD CONSTRAINT fk_sow_milestone_position_sow
        FOREIGN KEY (sow_id) REFERENCES sows (id),
    ADD CONSTRAINT fk_sow_milestone_position_milestone
        FOREIGN KEY (milestone_id) REFERENCES sow_milestones (id),
    ADD CONSTRAINT fk_sow_milestone_position_position
        FOREIGN KEY (position_id) REFERENCES lookup_values (id),
    ADD CONSTRAINT fk_sow_milestone_position_rate_card
        FOREIGN KEY (rate_card_id) REFERENCES rate_cards (id);
