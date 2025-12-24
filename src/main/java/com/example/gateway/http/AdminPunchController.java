package com.example.gateway.http;

import com.example.gateway.entity.PunchEntity;
import com.example.gateway.model.PunchDto;
import com.example.gateway.model.QueueHealthDto;
import com.example.gateway.repo.PunchRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminPunchController {

  private final PunchRepository punchRepo;

  public AdminPunchController(PunchRepository punchRepo) {
    this.punchRepo = punchRepo;
  }



  @GetMapping("/punches/recent")
  public List<PunchDto> recent(
          @RequestParam(value = "deviceSn", required = false) String deviceSn,
          @RequestParam(value = "status", required = false) String status,
          @RequestParam(value = "limit", defaultValue = "100") @Min(1) @Max(500) int limit
  ) {
    return punchRepo.recent(deviceSn, status, limit).stream().map(this::toDto).toList();
  }

  @PostMapping("/punches/{id}/requeue")
  public void requeue(@PathVariable("id") long id) {
    punchRepo.requeueById(id);
  }

  @PostMapping("/punches/requeue-dead")
  public void requeueDead(@RequestParam(value = "deviceSn", required = false) String deviceSn) {
    punchRepo.requeueDead(deviceSn);
  }

  // ✅ Cleanup endpoint removed (you want to keep punches forever)

  private PunchDto toDto(PunchEntity p) {
    return new PunchDto(
            p.getId(),
            p.getDeviceSn(),
            p.getEmpCode(),
            p.getTs(),
            p.getInoutStatus(),
            p.getStatus(),
            p.getRetryCount(),
            p.getLastError(),
            p.getCreatedAt(),
            p.getSentAt(),
            p.getNextAttemptAt()
    );
  }
}
