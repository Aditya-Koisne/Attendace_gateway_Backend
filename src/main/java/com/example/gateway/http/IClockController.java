package com.example.gateway.http;

import com.example.gateway.repo.DeviceRepository;
import com.example.gateway.service.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
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

  // ---------- CDATA (POST + GET) ----------
//  @RequestMapping(
//          value = {"/cdata", "/cdata.aspx"},
//          method = {RequestMethod.GET, RequestMethod.POST}
//  )
//  public String cdata(
//          @RequestParam(value = "SN", required = false) String sn,
//          @RequestParam(value = "sn", required = false) String snLower,
//          @RequestParam(value = "table", required = false) String table,
//          @RequestParam(value = "Stamp", required = false) String stamp,
//          @RequestParam(value = "options", required = false) String options,
//          @RequestBody(required = false) String body
//  ) {
//
//    String serial = (sn != null) ? sn : snLower;
//    if (serial == null) serial = "UNKNOWN";
//
//    log.info(
//            "ICLOCK CDATA: SN={} table={} stamp={} options={} bodyLen={}",
//            serial, table, stamp, options, body == null ? 0 : body.length()
//    );
//
//    deviceRepo.findById(serial).ifPresent(deviceRepo::save);
//
//    // ONLY ingest when ATTLOG
//    if ("ATTLOG".equalsIgnoreCase(table) && body != null && !body.isBlank()) {
//      ingestService.ingestAttlog(serial, body);
//    }
//
//    return "OK";
//  }

  @RequestMapping(
          value = {"/cdata", "/cdata.aspx"},
          method = {RequestMethod.GET, RequestMethod.POST}
  )
  public String cdata(
          @RequestParam(value = "SN", required = false) String sn,
          @RequestParam(value = "sn", required = false) String snLower,
          @RequestParam(value = "table", required = false) String table,
          @RequestParam(value = "Stamp", required = false) String stamp,
          @RequestParam(value = "options", required = false) String options,
          @RequestBody(required = false) String body,
          HttpServletRequest request
  ) {

    String serial = (sn != null) ? sn : snLower;
    if (serial == null) serial = "UNKNOWN";

    // 🔹 EXISTING LOG (unchanged)
    log.info(
            "ICLOCK CDATA: SN={} table={} stamp={} options={} bodyLen={}",
            serial, table, stamp, options, body == null ? 0 : body.length()
    );

    // 🔥 NEW: FULL RAW DEBUG LOG
    log.debug("---- ICLOCK RAW REQUEST START ----");

    // Headers
    Collections.list(request.getHeaderNames())
            .forEach(h -> log.debug("HEADER {} = {}", h, request.getHeader(h)));

    // Query params
    request.getParameterMap()
            .forEach((k, v) -> log.debug("PARAM {} = {}", k, String.join(",", v)));

    // Raw body (trimmed for safety)
    if (body != null) {
      log.debug("RAW BODY:\n{}", body);
    } else {
      log.debug("RAW BODY: <null>");
    }

    log.debug("---- ICLOCK RAW REQUEST END ----");

    // 🔹 EXISTING LOGIC (unchanged)
    deviceRepo.findById(serial).ifPresent(deviceRepo::save);

    if ("ATTLOG".equalsIgnoreCase(table) && body != null && !body.isBlank()) {
      ingestService.ingestAttlog(serial, body);
    }

    return "OK";
  }


//  // ---------- GETREQUEST ----------
//  @RequestMapping(
//          value = {"/getrequest", "/getrequest.aspx"},
//          method = RequestMethod.GET
//  )
//  public String getRequest(
//          @RequestParam(value = "SN", required = false) String sn,
//          @RequestParam(value = "sn", required = false) String snLower
//  ) {
//    String serial = (sn != null) ? sn : snLower;
//    log.info("ICLOCK GETREQUEST: SN={}", serial);
//    return "OK";
//  }

  @RequestMapping(
          value = {"/getrequest", "/getrequest.aspx"},
          method = RequestMethod.GET
  )
  public String getRequest(
          @RequestParam(value = "SN", required = false) String sn,
          @RequestParam(value = "sn", required = false) String snLower,
          HttpServletRequest request
  ) {
    String serial = (sn != null) ? sn : snLower;

    log.info("ICLOCK GETREQUEST: SN={}", serial);

    Collections.list(request.getHeaderNames())
            .forEach(h -> log.debug("HEADER {} = {}", h, request.getHeader(h)));

    request.getParameterMap()
            .forEach((k, v) -> log.debug("PARAM {} = {}", k, String.join(",", v)));

    return "OK";
  }

}

