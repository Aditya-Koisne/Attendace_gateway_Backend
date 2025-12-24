package com.example.gateway.service;

import com.example.gateway.entity.DeviceEntity;
import com.example.gateway.repo.DeviceRepository;
import com.example.gateway.repo.PunchBatchInserter;
import com.example.gateway.repo.PunchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class IngestService {
  private static final Logger log = LoggerFactory.getLogger(IngestService.class);
  private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final DeviceRepository deviceRepo;
  private final PunchRepository punchRepo;
  private final PunchBatchInserter batchInserter;

  public IngestService(DeviceRepository deviceRepo, PunchRepository punchRepo, PunchBatchInserter batchInserter) {
    this.deviceRepo = deviceRepo;
    this.punchRepo = punchRepo;
    this.batchInserter = batchInserter;
  }

  public int ingestAttlog(String deviceSn, String body) {
    if (body == null || body.isBlank()) return 0;

    Optional<DeviceEntity> devOpt = deviceRepo.findById(deviceSn);
    if (devOpt.isEmpty() || !devOpt.get().isEnabled()) {
      return 0;
    }

    List<ParsedLine> parsed = new ArrayList<>();
    for (String line : body.strip().split("\\R")) {
      ParsedLine p = parseLine(deviceSn, line);
      if (p != null) parsed.add(p);
    }
    if (parsed.isEmpty()) return 0;

    Map<Key, List<ParsedLine>> groups = new HashMap<>();
    for (ParsedLine p : parsed) {
      Key k = new Key(p.deviceSn, p.empCode, p.ts.toLocalDate());
      groups.computeIfAbsent(k, __ -> new ArrayList<>()).add(p);
    }

    List<PunchBatchInserter.PunchInsertRow> inserts = new ArrayList<>(parsed.size());

    for (var e : groups.entrySet()) {
      List<ParsedLine> lines = e.getValue();
      lines.sort(Comparator.comparing(a -> a.ts));

      String last = punchRepo.lastInOutForDay(e.getKey().deviceSn, e.getKey().empCode, e.getKey().day);
      String current = last;

      for (ParsedLine p : lines) {
        String next = (current == null || "OUT".equals(current)) ? "IN" : "OUT";
        current = next;

        inserts.add(new PunchBatchInserter.PunchInsertRow(
            p.deviceSn, p.empCode, p.ts,
            p.statusCode, p.verifyMode, p.workCode,
            p.rawLine, next
        ));
      }
    }

    int inserted = batchInserter.batchInsertIgnore(inserts);
    if (inserted > 0) log.info("INGEST: SN={} inserted={}", deviceSn, inserted);
    return inserted;
  }

  private ParsedLine parseLine(String deviceSn, String line) {
    if (line == null) return null;
    String raw = line.trim();
    if (raw.isBlank()) return null;

    String[] parts = raw.split("\\t");
    if (parts.length < 2) parts = raw.split("\\s+");
    if (parts.length < 2) return null;

    String emp = parts[0].trim();
    String tsStr = parts[1].trim();

    LocalDateTime ts;
    try { ts = LocalDateTime.parse(tsStr, TS_FMT); }
    catch (Exception ex) { return null; }

    String status = parts.length > 2 ? blankToNull(parts[2]) : null;
    String verify = parts.length > 3 ? blankToNull(parts[3]) : null;
    String work = parts.length > 4 ? blankToNull(parts[4]) : null;

    return new ParsedLine(deviceSn, emp, ts, status, verify, work, raw);
  }

  private String blankToNull(String s) {
    if (s == null) return null;
    s = s.trim();
    return s.isBlank() ? null : s;
  }

  private record ParsedLine(
      String deviceSn, String empCode, LocalDateTime ts,
      String statusCode, String verifyMode, String workCode,
      String rawLine
  ) {}

  private record Key(String deviceSn, String empCode, LocalDate day) {}
}
