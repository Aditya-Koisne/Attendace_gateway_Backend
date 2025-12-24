package com.example.gateway.http;

import com.example.gateway.service.DevicePresenceService;
import com.example.gateway.service.IngestService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class IClockController {

  private static final Logger log = LoggerFactory.getLogger(IClockController.class);

  private final IngestService ingest;
  private final DevicePresenceService presence;

  public IClockController(IngestService ingest, DevicePresenceService presence) {
    this.ingest = ingest;
    this.presence = presence;
  }

  @RequestMapping(
          value = {"/iclock/cdata.aspx", "/iclock/cdata"},
          method = {RequestMethod.GET, RequestMethod.POST},
          produces = MediaType.TEXT_PLAIN_VALUE
  )
  public String cdata(HttpServletRequest request, @RequestBody(required = false) String body) {
    String sn = firstNonNull(request.getParameter("SN"), request.getParameter("sn"), "UNKNOWN");
    String table = request.getParameter("table");

    // ✅ Mark presence for GET + POST traffic
    presence.markSeen(sn, request.getRemoteAddr());

    if (table != null && table.equalsIgnoreCase("ATTLOG")) {
      ingest.ingestAttlog(sn, body == null ? "" : body);
    }
    return "OK";
  }

  @GetMapping(value = {"/iclock/getrequest.aspx", "/iclock/getrequest"}, produces = MediaType.TEXT_PLAIN_VALUE)
  public String getrequest(HttpServletRequest request) {
    String sn = firstNonNull(request.getParameter("SN"), request.getParameter("sn"), "UNKNOWN");

    // ✅ Mark presence for device heartbeat calls
    presence.markSeen(sn, request.getRemoteAddr());

    return "OK";
  }

  @GetMapping(value = "/healthz", produces = MediaType.TEXT_PLAIN_VALUE)
  public String healthz() {
    return "OK";
  }

  private static String firstNonNull(String a, String b, String def) {
    if (a != null && !a.isBlank()) return a;
    if (b != null && !b.isBlank()) return b;
    return def;
  }
}
