-- Local single-user business storage. Keep this script idempotent so application
-- startup can safely initialize a fresh PostgreSQL database or an existing one.
CREATE TABLE IF NOT EXISTS travel_conversation (
    id              VARCHAR(36) PRIMARY KEY,
    owner_id        VARCHAR(100) NOT NULL DEFAULT 'local',
    title           VARCHAR(200) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_travel_conversation_owner_updated
    ON travel_conversation (owner_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS travel_message (
    id              VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL REFERENCES travel_conversation(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant')),
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_travel_message_conversation_created
    ON travel_message (conversation_id, created_at, id);

CREATE TABLE IF NOT EXISTS travel_plan (
    id              VARCHAR(36) PRIMARY KEY,
    owner_id        VARCHAR(100) NOT NULL DEFAULT 'local',
    conversation_id VARCHAR(36) REFERENCES travel_conversation(id) ON DELETE SET NULL,
    title           VARCHAR(200) NOT NULL,
    city            VARCHAR(120) NOT NULL,
    start_date      DATE,
    day_count       INTEGER NOT NULL CHECK (day_count BETWEEN 1 AND 14),
    budget          NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (budget >= 0),
    pace            VARCHAR(40) NOT NULL DEFAULT '',
    itinerary       JSONB NOT NULL,
    budget_summary  JSONB,
    version         INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_travel_plan_owner_updated
    ON travel_plan (owner_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_travel_plan_conversation
    ON travel_plan (conversation_id);
