-- V13: Add updated_at column to verification_log table
-- BaseEntity expects updatedAt field but the column was missing

ALTER TABLE verification_log
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

-- Also need to check if other tables have the same issue
-- Check candidate_push table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'candidate_push' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE candidate_push ADD COLUMN updated_at TIMESTAMPTZ;
    END IF;
END $$;

-- Check operation_log table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'operation_log' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE operation_log ADD COLUMN updated_at TIMESTAMPTZ;
    END IF;
END $$;
