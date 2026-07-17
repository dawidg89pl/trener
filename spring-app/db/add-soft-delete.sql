-- Execute once for an existing database before deploying the soft-delete version.
ALTER TABLE generated_workouts ADD COLUMN IF NOT EXISTS deleted boolean NOT NULL DEFAULT FALSE;
