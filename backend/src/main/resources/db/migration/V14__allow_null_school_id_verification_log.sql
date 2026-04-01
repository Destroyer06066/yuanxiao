-- V14: Allow NULL school_id for verification_log (for OP_ADMIN global verification)
ALTER TABLE verification_log ALTER COLUMN school_id DROP NOT NULL;
