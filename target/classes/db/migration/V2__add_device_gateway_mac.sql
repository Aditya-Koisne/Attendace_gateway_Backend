ALTER TABLE devices
ADD COLUMN IF NOT EXISTS gateway_mac_address VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_devices_gateway_mac
ON devices (gateway_mac_address);
