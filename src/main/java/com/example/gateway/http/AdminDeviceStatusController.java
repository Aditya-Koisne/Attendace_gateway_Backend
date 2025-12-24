package com.example.gateway.http;

import com.example.gateway.model.DeviceStatusDto;
import com.example.gateway.repo.DeviceRepository;
import com.example.gateway.service.DevicePresenceService;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/admin/devices")
public class AdminDeviceStatusController {

    private final DeviceRepository deviceRepo;
    private final DevicePresenceService presence;

    // consider device connected if seen within last 2 minutes
    private final Duration connectedThreshold = Duration.ofMinutes(2);

    public AdminDeviceStatusController(DeviceRepository deviceRepo, DevicePresenceService presence) {
        this.deviceRepo = deviceRepo;
        this.presence = presence;
    }

    @GetMapping("/status")
    public List<DeviceStatusDto> status() {
        return deviceRepo.findAll().stream().map(d -> {
            var p = presence.get(d.getSerialNumber());
            return new DeviceStatusDto(
                    d.getSerialNumber(),
                    presence.isConnected(p, connectedThreshold),
                    p == null ? null : p.getLastSeen(),
                    p == null ? null : p.getLastIp()
            );
        }).toList();
    }
}
