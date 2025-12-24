CREATE TABLE IF NOT EXISTS devices (
  serial_number TEXT PRIMARY KEY,
  name          TEXT NOT NULL,
  mac_address   TEXT NOT NULL,
  gateway_mac_address TEXT NOT NULL,
  cid           TEXT NOT NULL,
  enabled       BOOLEAN NOT NULL DEFAULT TRUE,
  last_seen_at  TIMESTAMPTZ,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Safe upgrades for existing databases
ALTER TABLE devices ADD COLUMN IF NOT EXISTS gateway_mac_address TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS punches (
  id BIGSERIAL PRIMARY KEY,
  device_sn TEXT NOT NULL,
  emp_code TEXT NOT NULL,
  ts TIMESTAMP NOT NULL,
  status_code TEXT,
  verify_mode TEXT,
  work_code TEXT,
  raw_line TEXT,
  inout_status TEXT,
  status TEXT NOT NULL DEFAULT 'pending',
  retry_count INT NOT NULL DEFAULT 0,
  last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  sent_at TIMESTAMPTZ,
  next_attempt_at TIMESTAMPTZ
);


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
