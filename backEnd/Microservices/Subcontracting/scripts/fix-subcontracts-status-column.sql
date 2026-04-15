-- Fix legacy status column definition for AI negotiation statuses
-- Error fixed: Data truncated for column 'status'

ALTER TABLE subcontracts
    MODIFY COLUMN status VARCHAR(32) NOT NULL;

