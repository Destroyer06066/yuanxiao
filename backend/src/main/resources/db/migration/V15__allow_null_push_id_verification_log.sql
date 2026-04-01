-- V15: Allow NULL push_id for verification_log (for certificate-only verification without push record)
ALTER TABLE verification_log ALTER COLUMN push_id DROP NOT NULL;
