# DECODER API - Step-by-Step Demo Guide

This guide will walk you through running the DECODER API project and demonstrating all its features.

---

## Prerequisites Check

### Step 1: Verify Java Installation
Open Command Prompt and run:
```bash
java -version
```
**Expected:** Should show Java 21 (or higher)
- If not installed, download from: https://www.oracle.com/java/technologies/downloads/#java21

### Step 2: Verify Maven Installation
```bash
mvn -version
```
**Expected:** Should show Maven 3.9+ (or higher)
- If not installed, download from: https://maven.apache.org/download.cgi

### Step 3: Verify Python Installation (for data generation script - optional)
```bash
python --version
```
**Expected:** Should show Python 3.8+ (or higher)
- If not installed, download from: https://www.python.org/downloads/

---

## Part 1: Build and Start the Application

### Step 1: Navigate to Project Directory
Open Command Prompt (or PowerShell) and navigate to the project:
```bash
cd C:\Users\Lenovo\Desktop\slice\decoder-api
```

### Step 2: Clean and Build the Project
```bash
mvn clean install
```
**Wait for:** "BUILD SUCCESS" message (takes 1-2 minutes on first run)

**What this does:**
- Downloads dependencies
- Compiles all Java code
- Runs unit tests
- Packages the application

### Step 3: Start the Application
```bash
mvn spring-boot:run
```
**Wait for:** You should see:
```
Started DecoderApiApplication in XX seconds
Initial data loading completed
```

**[WARN] Keep this terminal window open** - The application is running here!

**What happens:**
- Application starts on port 8080
- Database (decoder.db) is created automatically
- Initial users and buildings are created:
  - Users: `admin`, `owner1`, `owner2`
  - Buildings: Building A (owner1), Building B (owner1), Building C (owner2)

---

## Part 2: Demo the API Endpoints

**Open a NEW Command Prompt window** (keep the first one running the application)

### Demo 1: Ingest Sensor Reading

**Purpose:** Show how sensors send data to the system

#### Step 1: Send a sensor reading
In the NEW Command Prompt, run:
```bash
curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d "{\"buildingId\": 1, \"sensorId\": \"sensor-001\", \"timestamp\": \"2024-11-02T10:00:00\", \"value\": 50.5}"
```

**Expected Response:**
```json
{
  "buildingId": 1,
  "sensorId": "sensor-001",
  "timestamp": "2024-11-02T10:00:00",
  "value": 50.5
}
```

#### Step 2: Send more readings (to have data for forecast)
Run these commands one by one:
```bash
curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d "{\"buildingId\": 1, \"sensorId\": \"sensor-001\", \"timestamp\": \"2024-11-02T10:05:00\", \"value\": 55.0}"

curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d "{\"buildingId\": 1, \"sensorId\": \"sensor-001\", \"timestamp\": \"2024-11-02T10:10:00\", \"value\": 60.0}"

curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d "{\"buildingId\": 1, \"sensorId\": \"sensor-001\", \"timestamp\": \"2024-11-02T10:15:00\", \"value\": 58.0}"

curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d "{\"buildingId\": 1, \"sensorId\": \"sensor-001\", \"timestamp\": \"2024-11-02T10:20:00\", \"value\": 62.0}"
```

**What this demonstrates:**
- Sensors can send data without authentication
- Data is stored in the database
- Each reading has a building ID, sensor ID, timestamp, and value

---

### Demo 2: Retrieve Readings (Admin User)

**Purpose:** Show how users can retrieve data with authentication

#### Step 1: Get readings as Admin
```bash
curl -X GET "http://localhost:8080/buildings/1/last-readings?minutes=60" -H "Authorization: Bearer admin:ADMIN"
```

**Expected Response:**
```json
[
  {
    "buildingId": 1,
    "sensorId": "sensor-001",
    "timestamp": "2024-11-02T10:20:00",
    "value": 62.0
  },
  {
    "buildingId": 1,
    "sensorId": "sensor-001",
    "timestamp": "2024-11-02T10:15:00",
    "value": 58.0
  },
  ... (more readings)
]
```

**What this demonstrates:**
- Authentication is required (Authorization header)
- Admin can access any building
- Returns readings in reverse chronological order (newest first)

---

### Demo 3: Retrieve Readings (Owner User)

**Purpose:** Show how owners can only access their own buildings

#### Step 1: Get readings as Owner (own building)
```bash
curl -X GET "http://localhost:8080/buildings/1/last-readings?minutes=60" -H "Authorization: Bearer owner1:OWNER"
```

**Expected Response:** Same as above (owner1 owns Building 1)

#### Step 2: Try to access someone else's building
```bash
curl -X GET "http://localhost:8080/buildings/3/last-readings?minutes=60" -H "Authorization: Bearer owner1:OWNER"
```

**Expected Response:**
```
HTTP 403 Forbidden
```

**What this demonstrates:**
- Owner can access their own buildings
- Owner **cannot** access other owners' buildings
- RBAC (Role-Based Access Control) is working

---

### Demo 4: Get Forecast (Owner)

**Purpose:** Show how the system predicts future values

#### Step 1: Get forecast for building
```bash
curl -X GET "http://localhost:8080/buildings/1/forecast?minutes=60" -H "Authorization: Bearer owner1:OWNER"
```

**Expected Response:**
```json
{
  "buildingId": 1,
  "forecast": [
    {
      "timestamp": "2024-11-02T11:00:00",
      "value": 57.0
    },
    {
      "timestamp": "2024-11-02T11:01:00",
      "value": 57.0
    },
    ... (60 forecast points)
  ],
  "recommendation": "Energy consumption within normal range. No action required."
}
```

**What this demonstrates:**
- System calculates moving average of last 5 readings: (50.5 + 55.0 + 60.0 + 58.0 + 62.0) / 5 = 57.0
- Projects this average forward for next 60 minutes
- Since 57.0 < 100.0 (threshold), recommendation is "No action required"

---

### Demo 5: High Consumption Alert

**Purpose:** Show how the system alerts when consumption is high

#### Step 1: Send high consumption readings
```bash
curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d "{\"buildingId\": 1, \"sensorId\": \"sensor-001\", \"timestamp\": \"2024-11-02T10:25:00\", \"value\": 120.0}"

curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d "{\"buildingId\": 1, \"sensorId\": \"sensor-001\", \"timestamp\": \"2024-11-02T10:30:00\", \"value\": 125.0}"

curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d "{\"buildingId\": 1, \"sensorId\": \"sensor-001\", \"timestamp\": \"2024-11-02T10:35:00\", \"value\": 130.0}"

curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d "{\"buildingId\": 1, \"sensorId\": \"sensor-001\", \"timestamp\": \"2024-11-02T10:40:00\", \"value\": 128.0}"

curl -X POST http://localhost:8080/ingest -H "Content-Type: application/json" -d "{\"buildingId\": 1, \"sensorId\": \"sensor-001\", \"timestamp\": \"2024-11-02T10:45:00\", \"value\": 132.0}"
```

#### Step 2: Get forecast again
```bash
curl -X GET "http://localhost:8080/buildings/1/forecast?minutes=60" -H "Authorization: Bearer owner1:OWNER"
```

**Expected Response:**
```json
{
  "buildingId": 1,
  "forecast": [
    {
      "timestamp": "2024-11-02T11:00:00",
      "value": 127.0
    },
    ... (60 forecast points)
  ],
  "recommendation": "Forecast exceeds threshold (127.0 > 100.0). Recommendation: Activate energy-saving mode."
}
```

**What this demonstrates:**
- System recalculates forecast with new readings
- Average of last 5: (120 + 125 + 130 + 128 + 132) / 5 = 127.0
- Since 127.0 > 100.0 (threshold), system recommends action

---

## Part 3: Optional Demos

### Demo 6: Generate Test Data (Python Script)

**Purpose:** Quickly populate database with realistic test data

#### Step 1: Install Python dependencies
```bash
cd C:\Users\Lenovo\Desktop\slice
pip install -r scripts/requirements.txt
```

#### Step 2: Run data generation script
```bash
python scripts/generate_sensor_data.py
```

**Expected Output:**
```
==========================================
DECODER API - Sensor Data Generator
==========================================

Generating realistic sensor data for 3 buildings...
[OK] API is available

Generating data for Building A (ID: 1)
   Sensors: sensor-001, sensor-002, sensor-003
[OK] Sent reading: Building 1, Sensor sensor-001, Value: 45.23
[OK] Sent reading: Building 1, Sensor sensor-001, Value: 47.18
... (many more readings)

Generation complete!
Total readings generated: 90
Successfully sent: 90
Failed: 0
```

**What this demonstrates:**
- Automated data generation
- Multiple buildings and sensors
- Realistic sensor values

---

### Demo 7: Run E2E Tests

**Purpose:** Show automated testing of all endpoints

#### Step 1: Run E2E test script
```bash
cd C:\Users\Lenovo\Desktop\slice
bash tests/e2e_tests.sh
```

**Note:** If you don't have bash, you can run the tests manually using the curl commands above.

**Expected Output:**
```
==========================================
DECODER API - End-to-End Tests
==========================================

[OK] API is running

[PASS] PASS: Ingest reading - HTTP 201 Created
[PASS] PASS: Get last readings (admin) - HTTP 200 OK
[PASS] PASS: Get last readings (owner) - HTTP 200 OK
[PASS] PASS: Get forecast (admin) - HTTP 200 OK
[PASS] PASS: RBAC check - Owner correctly denied access
[PASS] PASS: Get forecast (owner) - HTTP 200 OK

==========================================
Test Summary
==========================================
Passed: 6
Failed: 0
==========================================
All tests passed!
```

---

## Part 4: Visual Demo Structure

### Recommended Presentation Flow:

1. **Introduction** (2 min)
   - "This is a building sensor data management system"
   - Shows architecture diagram
   - Explains: Ingest -> Store -> Query -> Forecast

2. **Data Ingestion** (2 min)
   - Show: `curl POST /ingest`
   - Explain: Sensors send data, no authentication needed
   - Send 5-10 readings

3. **Data Retrieval** (3 min)
   - Show: Admin accessing any building
   - Show: Owner accessing their building
   - Show: Owner trying to access someone else's building (403)
   - Explain: RBAC protects data

4. **Forecasting** (3 min)
   - Show: Get forecast with normal consumption
   - Explain: Moving average algorithm
   - Show: Get forecast with high consumption
   - Explain: Recommendation system

5. **Summary** (2 min)
   - Recap features
   - Show test results
   - Show architecture decisions (ADRs)

---

## Troubleshooting

### Issue: "Port 8080 already in use"
**Solution:** Change port in `application.yml`:
```yaml
server:
  port: 8081
```

### Issue: "Connection refused"
**Solution:** Make sure the application is running in the first terminal

### Issue: "curl not recognized"
**Solution:** 
- Use PowerShell instead of Command Prompt
- Or install Git Bash and use that
- Or use Postman/REST Client extension

### Issue: "BUILD FAILURE"
**Solution:**
1. Make sure Java 21 is installed
2. Run `mvn clean install` again
3. Check for error messages

### Issue: "Database locked"
**Solution:** Delete `decoder.db` file and restart application

---

## Quick Reference Card

### Endpoints:
```
POST   /ingest                              (No auth)
GET    /buildings/:id/last-readings         (Auth required)
GET    /buildings/:id/forecast              (Auth required)
```

### Authentication:
```
Admin: Bearer admin:ADMIN
Owner: Bearer owner1:OWNER
Owner: Bearer owner2:OWNER
```

### Example Requests:
```bash
# Ingest
curl -X POST http://localhost:8080/ingest \
  -H "Content-Type: application/json" \
  -d '{"buildingId": 1, "sensorId": "sensor-001", "timestamp": "2024-11-02T10:00:00", "value": 50.5}'

# Get readings (Admin)
curl -X GET "http://localhost:8080/buildings/1/last-readings?minutes=60" \
  -H "Authorization: Bearer admin:ADMIN"

# Get forecast (Owner)
curl -X GET "http://localhost:8080/buildings/1/forecast?minutes=60" \
  -H "Authorization: Bearer owner1:OWNER"
```

---

## Presentation Tips

1. **Start Simple:** Begin with basic data ingestion
2. **Build Complexity:** Show security, then forecasting
3. **Highlight Features:** Emphasize RBAC, forecasting, architecture
4. **Show Errors:** Demonstrate 403 Forbidden for unauthorized access
5. **Explain Algorithm:** Show how moving average works
6. **Show Tests:** Run unit/integration tests to show code quality

---

## Success Criteria

**Demo is successful if:**
- Application starts without errors
- Can ingest sensor readings
- Can retrieve readings with authentication
- RBAC prevents unauthorized access
- Forecast is generated correctly
- Recommendation appears when threshold exceeded

---

**Ready to demo?** Follow the steps above and you'll have a complete demonstration of the DECODER API!

