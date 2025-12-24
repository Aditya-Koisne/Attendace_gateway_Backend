package com.example.gateway.http;

import com.example.gateway.model.QueueHealthDto;
import com.example.gateway.repo.PunchRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/queue")
public class AdminQueueController {

    private final PunchRepository punchRepo;

    public AdminQueueController(PunchRepository punchRepo) {
        this.punchRepo = punchRepo;
    }

    @GetMapping("/health")
    public QueueHealthDto health() {
        long total = punchRepo.countAll();
        long pending = punchRepo.countByStatus("pending");
        long processing = punchRepo.countByStatus("processing");
        long sent = punchRepo.countByStatus("sent");
        long dead = punchRepo.countByStatus("dead");

        return new QueueHealthDto(
                total,
                pending,
                processing,
                sent,
                dead,
                punchRepo.lastSuccess(),
                punchRepo.lastRetryingError()
        );
    }
}
