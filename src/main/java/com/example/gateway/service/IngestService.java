package com.example.gateway.service;

import com.example.gateway.repo.DeviceRepository;
import com.example.gateway.repo.PunchBatchInserter;
import com.example.gateway.repo.PunchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestService {
  private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private final DeviceRepository deviceRepo;
  private final PunchRepository punchRepo;
  private final PunchBatchInserter batchInserter;

  public int ingestAttlog(String sn, String body) {
    if (body == null || body.isBlank()) return 0;
    if (deviceRepo.findById(sn).filter(d -> d.isEnabled()).isEmpty()) return 0;

    List<PunchBatchInserter.PunchInsertRow> rows = new ArrayList<>();
    Map<LocalDate, List<Parsed>> byDay = new HashMap<>();

    for (String line : body.strip().split("\\R")) {
      Parsed p = parse(sn, line);
      if (p != null) byDay.computeIfAbsent(p.ts.toLocalDate(), k -> new ArrayList<>()).add(p);
    }

    for (var entry : byDay.entrySet()) {
      List<Parsed> dayPunches = entry.getValue();
      dayPunches.sort(Comparator.comparing(p -> p.ts));

      // Calculate In/Out logic
      String lastStatus = punchRepo.lastInOutForDay(sn, dayPunches.get(0).emp, entry.getKey());
      String current = lastStatus;

      for (Parsed p : dayPunches) {
        String next = (current == null || "OUT".equals(current)) ? "IN" : "OUT";
        current = next;
        rows.add(new PunchBatchInserter.PunchInsertRow(sn, p.emp, p.ts, p.status, p.verify, p.work, p.raw, next));
      }
    }
    int inserted = batchInserter.batchInsertIgnore(rows);
    log.info("SN={} parsed={} inserted={}", sn, rows.size(), inserted);
    return inserted;
  }


  private record Parsed(String emp, LocalDateTime ts, String status, String verify, String work, String raw) {}

  private Parsed parse(String sn, String line) {
    try {
      String[] parts = line.trim().split("\\t");
      if (parts.length < 2) parts = line.trim().split("\\s+");
      if (parts.length < 2) return null;

      LocalDateTime ts = LocalDateTime.parse(parts[1], TS_FMT);
      return new Parsed(parts[0], ts,
              parts.length > 2 ? parts[2] : null,
              parts.length > 3 ? parts[3] : null,
              parts.length > 4 ? parts[4] : null,
              line.trim());
    } catch (Exception e) { return null; }
  }
}
