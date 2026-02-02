package com.example.gateway.service;

import com.example.gateway.repo.DeviceRepository;
import com.example.gateway.repo.PunchBatchInserter;
import com.example.gateway.repo.PunchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestService {

  // ================= CONSTANTS =================
  private static final DateTimeFormatter TS_FMT =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // ================= DEPENDENCIES =================
  private final DeviceRepository deviceRepo;
  private final PunchRepository punchRepo;
  private final PunchBatchInserter batchInserter;

  // ================= ENTRY POINT =================
  public int ingestAttlog(String sn, String body) {

    // 1️⃣ Guard: empty body
    if (body == null || body.isBlank()) {
      return 0;
    }

    // 2️⃣ Guard: device must exist and be enabled
    if (deviceRepo.findById(sn).filter(d -> d.isEnabled()).isEmpty()) {
      return 0;
    }

    // 3️⃣ Parse + group punches BY EMPLOYEE
    Map<String, List<Parsed>> punchesByEmployee = new HashMap<>();

    for (String line : body.strip().split("\\R")) {
      Parsed parsed = parse(line);
      if (parsed == null) continue;

      punchesByEmployee
              .computeIfAbsent(parsed.emp, k -> new ArrayList<>())
              .add(parsed);
    }

    // 4️⃣ Prepare rows for batch insert
    List<PunchBatchInserter.PunchInsertRow> rows = new ArrayList<>();

    // 5️⃣ Process EACH employee independently
    for (Map.Entry<String, List<Parsed>> entry : punchesByEmployee.entrySet()) {

      String emp = entry.getKey();
      List<Parsed> punches = entry.getValue();

      // 5.1 Sort punches by timestamp (CRITICAL)
      punches.sort(Comparator.comparing(p -> p.ts));

      // 5.2 Fetch last known IN/OUT BEFORE first punch
      String current =
              punchRepo.lastInOutBefore(sn, emp, punches.get(0).ts);

      // 5.3 Decide IN / OUT for each punch
      for (Parsed p : punches) {

        String next =
                (current == null || "OUT".equals(current)) ? "IN" : "OUT";

        current = next;

        rows.add(new PunchBatchInserter.PunchInsertRow(
                sn,             // device_sn
                emp,            // emp_code
                p.ts,            // timestamp
                p.status,        // status code
                p.verify,        // verify mode
                p.work,          // work code
                p.raw,           // raw line
                next             // IN / OUT (DECIDED HERE)
        ));
      }
    }


    int inserted = batchInserter.batchInsertIgnore(rows);

    log.info("SN={} parsed={} inserted={}", sn, rows.size(), inserted);

    return inserted;
  }

  // ================= INTERNAL MODEL =================
  private record Parsed(
          String emp,
          LocalDateTime ts,
          String status,
          String verify,
          String work,
          String raw
  ) {}

  // ================= PARSER =================
  private Parsed parse(String line) {
    try {
      String[] parts = line.trim().split("\\t");
      if (parts.length < 2) {
        parts = line.trim().split("\\s+");
      }
      if (parts.length < 2) {
        return null;
      }

      LocalDateTime ts = LocalDateTime.parse(parts[1], TS_FMT);

      return new Parsed(
              parts[0],                 // emp_code (Enroll ID)
              ts,                       // timestamp
              parts.length > 2 ? parts[2] : null,
              parts.length > 3 ? parts[3] : null,
              parts.length > 4 ? parts[4] : null,
              line.trim()               // raw line
      );
    } catch (Exception e) {
      return null;
    }
  }
}
