// src/main/java/com/example/gateway/http/AdminDeviceController.java
package com.example.gateway.http;

import com.example.gateway.entity.DeviceEntity;
import com.example.gateway.model.DeviceDto;
import com.example.gateway.repo.DeviceRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

  @PutMapping("/{sn}")
  public void upsert(@PathVariable("sn") @NotBlank String sn,
                     @RequestBody @Valid DeviceDto body) {

    DeviceEntity e = repo.findById(sn).orElseGet(DeviceEntity::new);

    e.setSerialNumber(sn);
    e.setName(body.name() == null || body.name().isBlank() ? sn : body.name());
    e.setMacAddress(body.macAddress() == null || body.macAddress().isBlank() ? "00-00-00-00-00-00" : body.macAddress());
    e.setCid(body.cid() == null || body.cid().isBlank() ? "000" : body.cid());

    // REQUIRED per device (your payload includes it)
    e.setGatewayMacAddress(body.gatewayMacAddress() == null || body.gatewayMacAddress().isBlank()
            ? "00-00-00-00-00-00"
            : body.gatewayMacAddress());

    e.setEnabled(body.enabled());

//     If you removed deviceId from entity, do NOT set it here.
     e.setDeviceId(body.id());

    repo.save(e);
  }

  @DeleteMapping("/{sn}")
  public void delete(@PathVariable("sn") @NotBlank String sn) {
    repo.deleteById(sn);
  }

  private DeviceDto toDto(DeviceEntity e) {
    return new DeviceDto(
            e.getDeviceId(),
            e.getName(),
            e.getSerialNumber(),
            e.getMacAddress(),
            e.getCid(),
            e.getGatewayMacAddress(),
            e.isEnabled()
    );
  }
}
