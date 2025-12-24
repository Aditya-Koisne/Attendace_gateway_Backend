package com.example.gateway.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DevicePresenceService {

    public static class Presence {
        private volatile Instant lastSeen;
        private volatile String lastIp;

        public Presence(Instant lastSeen, String lastIp) {
            this.lastSeen = lastSeen;
            this.lastIp = lastIp;
        }

        public Instant getLastSeen() { return lastSeen; }
        public String getLastIp() { return lastIp; }

        public void mark(Instant ts, String ip) {
            this.lastSeen = ts;
            this.lastIp = ip;
        }
    }

    private final ConcurrentHashMap<String, Presence> map = new ConcurrentHashMap<>();

    public void markSeen(String serialNumber, String ip) {
        if (serialNumber == null || serialNumber.isBlank()) return;
        map.compute(serialNumber, (sn, p) -> {
            if (p == null) return new Presence(Instant.now(), ip);
            p.mark(Instant.now(), ip);
            return p;
        });
    }

    public Presence get(String sn) {
        return map.get(sn);
    }

    public Map<String, Presence> snapshot() {
        return Map.copyOf(map);
    }

    public boolean isConnected(Presence p, Duration threshold) {
        if (p == null || p.getLastSeen() == null) return false;
        return Duration.between(p.getLastSeen(), Instant.now()).compareTo(threshold) <= 0;
    }
}
