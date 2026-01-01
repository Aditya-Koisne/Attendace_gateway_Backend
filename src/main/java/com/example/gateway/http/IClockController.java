package com.example.gateway.http;

import com.example.gateway.repo.DeviceRepository;
import com.example.gateway.service.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/iclock")
public class IClockController {

  private static final Logger log = LoggerFactory.getLogger(IClockController.class);

  private final DeviceRepository deviceRepo;
  private final IngestService ingestService;

  public IClockController(DeviceRepository deviceRepo, IngestService ingestService) {
    this.deviceRepo = deviceRepo;
    this.ingestService = ingestService;
  }

  @PostMapping("/cdata")
  public String ingest(
          @RequestParam("SN") String sn,
          @RequestBody(required = false) String body
  ) {
    log.info("DEVICE HIT: SN={} BODY=\n{}", sn, body);

    // ✅ update last seen
    deviceRepo.findById(sn).ifPresent(d -> {
      deviceRepo.save(d);
      log.debug("Updated last seen for device: {}", sn);
    });

    // ✅ ingest the data
    ingestService.ingestAttlog(sn, body);

    return "OK";
  }
}