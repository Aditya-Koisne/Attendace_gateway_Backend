CREATE TABLE IF NOT EXISTS devices (
  serial_number TEXT PRIMARY KEY,
  device_id     TEXT,
  name          TEXT NOT NULL,
  mac_address   TEXT NOT NULL,
  cid           TEXT NOT NULL,
  enabled       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS punches (
  id              BIGSERIAL PRIMARY KEY,
  device_sn       TEXT NOT NULL,
  emp_code        TEXT NOT NULL,
  ts              TIMESTAMP NOT NULL,
  status_code     TEXT,
  status_code_norm TEXT GENERATED ALWAYS AS (COALESCE(status_code, '')) STORED,
  verify_mode     TEXT,
  work_code       TEXT,
  raw_line        TEXT NOT NULL,
  inout_status    TEXT NOT NULL, -- IN/OUT
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  status          TEXT NOT NULL DEFAULT 'pending',  -- pending/processing/sent/dead
  sent_at         TIMESTAMPTZ,
  processing_started_at TIMESTAMPTZ,
  retry_count     INT NOT NULL DEFAULT 0,
  last_error      TEXT,
  next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE punches
  ADD CONSTRAINT uq_punch_dedupe UNIQUE (device_sn, emp_code, ts, status_code_norm);

CREATE INDEX IF NOT EXISTS idx_punch_status_nextattempt
  ON punches (status, next_attempt_at, ts);

CREATE INDEX IF NOT EXISTS idx_punch_device_status_ts
  ON punches (device_sn, status, ts DESC);

CREATE INDEX IF NOT EXISTS idx_punch_sent_at
  ON punches (sent_at)
  WHERE status='sent';

CREATE INDEX IF NOT EXISTS idx_punch_created_at_pending
  ON punches (created_at)
  WHERE status='pending';

CREATE INDEX IF NOT EXISTS idx_punch_created_at_dead
  ON punches (created_at)
  WHERE status='dead';
