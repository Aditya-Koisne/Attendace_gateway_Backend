package com.example.gateway.http;

import com.example.gateway.model.MetricsResponse;
import com.example.gateway.model.PunchDto;
import com.example.gateway.repo.PunchRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@RestController
//@RequestMapping("/api/admin")
//public class AdminMetricsController {
//
//  private final PunchRepository repo;
//
//  public AdminMetricsController(PunchRepository repo) {
//    this.repo = repo;
//  }
//
//  @GetMapping("/metrics")
//  public MetricsResponse metrics(
//          @RequestParam(value = "deviceSn", required = false) String deviceSn
//  ) {
//    List<PunchDto> recent = repo.recent(deviceSn, null, 20).stream()
//            .map(p -> new PunchDto(
//                    p.getId(),
//                    p.getDeviceSn(),
//                    p.getEmpCode(),
//                    p.getTs(),
//                    p.getInoutStatus(),
//                    p.getStatus(),
//                    p.getRetryCount(),
//                    p.getLastError(),
//                    p.getCreatedAt(),
//                    p.getSentAt(),
//                    p.getNextAttemptAt()
//            ))
//            .toList();
//
//    return new MetricsResponse(
//            repo.countAll(),
//            repo.countByStatus("pending"),
//            repo.countByStatus("processing"),
//            repo.countByStatus("sent"),
//            repo.countByStatus("dead"),
//            repo.lastSuccess(),
//            repo.lastRetryingError(),
//            recent
//    );
//  }
//}
