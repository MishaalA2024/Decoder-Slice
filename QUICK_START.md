# DECODER API - Quick Start Guide

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Python 3.9+ (to run the comprehensive test)

## 1) Start the API (Spring Boot)

1. Open a terminal and navigate to the API module:
   ```bash
   cd decoder-api
   ```
2. Build (optional) and run:
   ```bash
   mvn clean package -DskipTests
   mvn spring-boot:run
   ```
3. Wait until the app says it started, then verify health:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   Expected: `{"status":"UP"}`

Notes:
- The app uses an in-memory SQLite database by default; initial users and buildings are auto-seeded on startup.
- Public endpoints: `/ingest`, `/actuator/health`, `/error`. All others require an Authorization header.

## 2) Quick API checks

- Ingest a reading (no auth required):
  ```bash
  curl -X POST http://localhost:8080/ingest \
    -H "Content-Type: application/json" \
    -d '{
      "buildingId": 1,
      "sensorId": "sensor-001",
      "timestamp": "2025-01-01T10:30:00",
      "value": 75.5
    }'
  ```
- Get last readings (admin):
  ```bash
  curl -H "Authorization: Bearer admin:ADMIN" \
    "http://localhost:8080/buildings/1/last-readings?minutes=60"
  ```
- Get forecast (owner):
  ```bash
  curl -H "Authorization: Bearer owner1:OWNER" \
    "http://localhost:8080/buildings/1/forecast?minutes=60"
  ```

## 3) Run the comprehensive interactive test

1. Open a new terminal and navigate to the tests folder:
   ```bash
   cd tests
   ```
2. Install Python dependency:
   ```bash
   pip install requests
   ```
3. Run the test menu (ensure the API is running):
   ```bash
   python comprehensive_test.py
   ```
   - Choose 1..7 to run a single scenario, or 8 to run all.
   - On Windows terminals that support ANSI colors, you can enable colors:
     ```bash
     # PowerShell
     $env:USE_COLORS="1"
     python comprehensive_test.py
     ```

## Authentication model (for testing)

- Use header: `Authorization: Bearer username:role`
- Default users (auto-created on startup):
  - `admin` (role: ADMIN)
  - `owner1` (role: OWNER)
  - `owner2` (role: OWNER)

## Ports and configuration

- Default port is 8080. To change at runtime:
  ```bash
  mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
  ```
- If you change the port, update `BASE_URL` in `tests/comprehensive_test.py` accordingly.

## Troubleshooting

- API not reachable: ensure `mvn spring-boot:run` is running and port 8080 is free.
- 401/403 on protected endpoints: send the `Authorization: Bearer username:role` header; ensure users are seeded by restarting the API.
- Java version: verify you are using Java 21:
  ```bash
  java -version
  ```

## Optional: Project tests and static analysis

- Run Java tests:
  ```bash
  mvn test
  ```
- Run SpotBugs:
  ```bash
  mvn spotbugs:check
  ```

---

Share these steps along with the repository so others can run the API and the comprehensive test quickly.
