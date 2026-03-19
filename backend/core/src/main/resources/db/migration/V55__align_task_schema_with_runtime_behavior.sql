-- Align task persistence with the current application behavior.
-- This migration preserves recurring-series support, string-based date/time
-- handling, frontend task categories, and scheduled notifications.

ALTER TABLE IF EXISTS tasks
    ADD COLUMN IF NOT EXISTS parent_task_id BIGINT,
    ADD COLUMN IF NOT EXISTS is_completed BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS created_at BIGINT,
    ADD COLUMN IF NOT EXISTS updated_at BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
          AND column_name = 'iscompleted'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
          AND column_name = 'is_completed'
    ) THEN
        ALTER TABLE tasks RENAME COLUMN iscompleted TO is_completed;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
          AND column_name = 'date'
          AND data_type NOT IN ('character varying', 'text')
    ) THEN
        ALTER TABLE tasks
            ALTER COLUMN date TYPE VARCHAR(255) USING date::text;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
          AND column_name = 'time_of_day'
    ) THEN
        ALTER TABLE tasks ALTER COLUMN time_of_day DROP NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
          AND column_name = 'time_of_day'
          AND data_type NOT IN ('character varying', 'text')
    ) THEN
        ALTER TABLE tasks
            ALTER COLUMN time_of_day TYPE VARCHAR(32) USING time_of_day::text;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
          AND column_name = 'task_type'
    ) THEN
        ALTER TABLE tasks
            ALTER COLUMN task_type TYPE VARCHAR(100);
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
          AND column_name = 'days_of_week'
    ) THEN
        ALTER TABLE tasks ALTER COLUMN days_of_week DROP NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
          AND column_name = 'days_of_week'
          AND data_type NOT IN ('character varying', 'text')
    ) THEN
        ALTER TABLE tasks
            ALTER COLUMN days_of_week TYPE TEXT USING days_of_week::text;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
          AND column_name = 'created_at'
          AND data_type <> 'bigint'
    ) THEN
        ALTER TABLE tasks
            ALTER COLUMN created_at TYPE BIGINT
                USING CASE
                    WHEN created_at IS NULL THEN NULL
                    ELSE (EXTRACT(EPOCH FROM created_at) * 1000)::BIGINT
                END;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'tasks'
          AND column_name = 'updated_at'
          AND data_type <> 'bigint'
    ) THEN
        ALTER TABLE tasks
            ALTER COLUMN updated_at TYPE BIGINT
                USING CASE
                    WHEN updated_at IS NULL THEN NULL
                    ELSE (EXTRACT(EPOCH FROM updated_at) * 1000)::BIGINT
                END;
    END IF;
END $$;

ALTER TABLE IF EXISTS tasks
    ALTER COLUMN is_completed SET DEFAULT FALSE,
    ALTER COLUMN created_at SET DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    ALTER COLUMN updated_at SET DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT;

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT tc.constraint_name
    INTO constraint_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.constraint_column_usage ccu
      ON tc.constraint_name = ccu.constraint_name
     AND tc.table_schema = ccu.table_schema
    WHERE tc.table_name = 'tasks'
      AND tc.constraint_type = 'CHECK'
      AND ccu.column_name = 'task_type'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE tasks DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_name = 'tasks'
          AND constraint_name = 'fk_tasks_parent_task'
    ) THEN
        ALTER TABLE tasks
            ADD CONSTRAINT fk_tasks_parent_task
                FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_tasks_patient_id ON tasks(patient_id);
CREATE INDEX IF NOT EXISTS idx_tasks_parent_task_id ON tasks(parent_task_id);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'templates'
          AND column_name = 'time_of_day'
          AND data_type NOT IN ('character varying', 'text')
    ) THEN
        ALTER TABLE templates
            ALTER COLUMN time_of_day TYPE VARCHAR(32) USING time_of_day::text;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'templates'
          AND column_name = 'icon'
          AND data_type <> 'integer'
    ) THEN
        ALTER TABLE templates
            ALTER COLUMN icon TYPE INTEGER USING icon::integer;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS scheduled_notification (
    id BIGSERIAL PRIMARY KEY,
    receiver_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    notification_type VARCHAR(255),
    scheduled_time TIMESTAMP NOT NULL,
    sent_time TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    message_id VARCHAR(255),
    error_message VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    task_id BIGINT NOT NULL,
    CONSTRAINT fk_scheduled_notification_task
        FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_scheduled_notification_task_id
    ON scheduled_notification(task_id);
