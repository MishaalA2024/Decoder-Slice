#!/usr/bin/env python3
"""
Script to generate realistic sensor data for 2-3 buildings.
Generates data for the DECODER API ingest endpoint.
"""

import requests
import json
import random
from datetime import datetime, timedelta
import time
import sys

BASE_URL = "http://localhost:8080"

# Building IDs (assuming they exist in the database)
BUILDINGS = [
    {"id": 1, "name": "Building A", "sensors": ["sensor-001", "sensor-002", "sensor-003"]},
    {"id": 2, "name": "Building B", "sensors": ["sensor-004", "sensor-005"]},
    {"id": 3, "name": "Building C", "sensors": ["sensor-006", "sensor-007", "sensor-008"]}
]

def generate_realistic_value(base_value=50.0, variance=20.0, trend=0.0):
    """
    Generate realistic sensor values with some variance and optional trend.
    
    Args:
        base_value: Base energy consumption value
        variance: Random variance range
        trend: Optional trend (positive = increasing, negative = decreasing)
    
    Returns:
        Realistic sensor reading value
    """
    # Add some randomness and optional trend
    value = base_value + random.uniform(-variance, variance) + trend
    # Ensure non-negative
    return max(0.0, round(value, 2))

def generate_readings_for_building(building_id, sensor_id, num_readings=10, start_time=None):
    """
    Generate multiple readings for a sensor in a building.
    
    Args:
        building_id: ID of the building
        sensor_id: ID of the sensor
        num_readings: Number of readings to generate
        start_time: Starting timestamp (defaults to now)
    
    Returns:
        List of reading dictionaries
    """
    if start_time is None:
        start_time = datetime.now()
    
    readings = []
    base_value = random.uniform(40.0, 80.0)  # Different base per sensor
    
    for i in range(num_readings):
        timestamp = start_time - timedelta(minutes=num_readings - i)
        value = generate_realistic_value(base_value, variance=15.0, 
                                        trend=random.uniform(-1.0, 1.0))
        
        reading = {
            "buildingId": building_id,
            "sensorId": sensor_id,
            "timestamp": timestamp.isoformat(),
            "value": value
        }
        readings.append(reading)
    
    return readings

def send_reading(reading):
    """
    Send a reading to the ingest endpoint.
    
    Args:
        reading: Reading dictionary
    
    Returns:
        True if successful, False otherwise
    """
    url = f"{BASE_URL}/ingest"
    headers = {"Content-Type": "application/json"}
    
    try:
        response = requests.post(url, json=reading, headers=headers, timeout=5)
        if response.status_code == 201:
            print(f"[OK] Sent reading: Building {reading['buildingId']}, "
                  f"Sensor {reading['sensorId']}, Value: {reading['value']}")
            return True
        else:
            print(f"[FAIL] Failed to send reading: {response.status_code} - {response.text}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"[FAIL] Error sending reading: {e}")
        return False

def main():
    """
    Main function to generate sensor data for all buildings.
    """
    print("=" * 60)
    print("DECODER API - Sensor Data Generator")
    print("=" * 60)
    print(f"\nGenerating realistic sensor data for {len(BUILDINGS)} buildings...")
    print(f"Target API: {BASE_URL}\n")
    
    # Check if API is available
    try:
        response = requests.get(f"{BASE_URL}/actuator/health", timeout=2)
        print(f"[OK] API is available\n")
    except requests.exceptions.RequestException:
        print(f"[WARN] Warning: API at {BASE_URL} is not available.")
        print("  Make sure the API is running before generating data.\n")
        response = input("Continue anyway? (y/n): ")
        if response.lower() != 'y':
            sys.exit(1)
    
    total_readings = 0
    successful_readings = 0
    
    # Generate readings for each building
    for building in BUILDINGS:
        building_id = building["id"]
        building_name = building["name"]
        sensors = building["sensors"]
        
        print(f"\n📊 Generating data for {building_name} (ID: {building_id})")
        print(f"   Sensors: {', '.join(sensors)}")
        
        for sensor_id in sensors:
            # Generate 10 readings per sensor (spread over last hour)
            readings = generate_readings_for_building(
                building_id, 
                sensor_id, 
                num_readings=10,
                start_time=datetime.now()
            )
            
            for reading in readings:
                total_readings += 1
                if send_reading(reading):
                    successful_readings += 1
                # Small delay to avoid overwhelming the API
                time.sleep(0.1)
    
    print("\n" + "=" * 60)
    print(f"Generation complete!")
    print(f"Total readings generated: {total_readings}")
    print(f"Successfully sent: {successful_readings}")
    print(f"Failed: {total_readings - successful_readings}")
    print("=" * 60)

if __name__ == "__main__":
    main()
