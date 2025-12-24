# Attendance Gateway Backend (Java 21, Spring Boot 3, Spring Data JPA)

This backend:
- Receives ZKTeco iClock ATTLOG pushes on `/iclock/cdata.aspx` and `/iclock/cdata`
- Stores punches in Postgres with dedupe and a queue state machine
- Sends to Sentry using parallel workers with `FOR UPDATE SKIP LOCKED`
- Uses exponential backoff + jitter with `next_attempt_at`
- Retention:
  - sent: 15 days
  - pending: 30 days
  - dead: 30 days
- Admin APIs (HTTP Basic, single admin user):
  - device CRUD
  - recent punches + filters
  - requeue dead/pending

## Prerequisites
- Java 21
- Maven
- Docker + Docker Compose

## 1) Start Postgres
```bash
docker compose up -d
```

## 2) Set admin credentials (required)
```bash
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD='ChangeThisStrongPassword!'
```

## 3) Run the app
```bash
mvn spring-boot:run
```

App runs on: http://localhost:7890

## 4) Devices are auto-seeded
The `devices:` list from `application.yml` is seeded to DB at startup.

List devices:
```bash
curl -u admin:ChangeThisStrongPassword! http://localhost:7890/api/admin/devices
```

## 5) Monitoring
- Health: `GET /actuator/health` (public)
- Prometheus: `GET /actuator/prometheus` (admin auth required)

```bash
curl -u admin:ChangeThisStrongPassword! http://localhost:7890/actuator/prometheus
```

## Production HTTPS
Terminate HTTPS at Nginx (recommended) and reverse-proxy to Spring Boot on 127.0.0.1:7890.
Also restrict `/iclock/**` to device networks (firewall/Nginx allowlist).
