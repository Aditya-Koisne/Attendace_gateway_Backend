package com.example.gateway.http;

import com.example.gateway.entity.DeviceEntity;
import com.example.gateway.model.DeviceDto;
import com.example.gateway.repo.DeviceRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/devices")
public class AdminDeviceController {

  private final DeviceRepository repo;

  public AdminDeviceController(DeviceRepository repo) {
    this.repo = repo;
  }

  @GetMapping
  public List<DeviceDto> list() {
    return repo.findAll().stream().map(this::toDto).toList();
  }

  // ✅ USE POST (NOT PUT) – fixes your 405 error
  @PostMapping
  public void create(@RequestBody @Valid DeviceDto body) {
    DeviceEntity e = new DeviceEntity();
    e.setSerialNumber(body.serialNumber());
    e.setName(body.name());
    e.setMacAddress(body.macAddress());
    e.setCid(body.cid());
    e.setGatewayMacAddress(body.gatewayMacAddress());
    e.setEnabled(body.enabled());
    repo.save(e);
  }

  @DeleteMapping("/{sn}")
  public void delete(@PathVariable String sn) {
    repo.deleteById(sn);
  }

  private DeviceDto toDto(DeviceEntity e) {
    return new DeviceDto(
            e.getSerialNumber(),
            e.getName(),
            e.getMacAddress(),
            e.getCid(),
            e.getGatewayMacAddress(),
            e.isEnabled()
    );
  }
}
