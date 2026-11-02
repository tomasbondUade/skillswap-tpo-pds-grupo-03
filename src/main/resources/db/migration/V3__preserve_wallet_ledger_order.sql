-- A UUID is an identifier, not the insertion order of a ledger.
-- Preserve the previous read order for legacy rows. This cannot repair an
-- already inconsistent legacy ledger; those rows require a separate audit.
ALTER TABLE wallet_ledger
    ADD COLUMN movement_index INT NULL,
    MODIFY COLUMN occurred_at DATETIME(6) NOT NULL;

CREATE TEMPORARY TABLE skillswap_ledger_positions AS
    SELECT id, ROW_NUMBER() OVER (
        PARTITION BY user_id ORDER BY occurred_at, id
    ) - 1 AS movement_index
    FROM wallet_ledger;

UPDATE wallet_ledger l
    JOIN skillswap_ledger_positions p ON p.id = l.id
    SET l.movement_index = p.movement_index;

DROP TEMPORARY TABLE skillswap_ledger_positions;

ALTER TABLE wallet_ledger
    MODIFY COLUMN movement_index INT NOT NULL,
    ADD CONSTRAINT uq_wallet_ledger_position UNIQUE (user_id, movement_index),
    ADD CONSTRAINT chk_wallet_ledger_position CHECK (movement_index >= 0);
