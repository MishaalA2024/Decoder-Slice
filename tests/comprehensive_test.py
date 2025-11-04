#!/usr/bin/env python3
"""
Comprehensive Test Script for DECODER API
Tests all functionality: Data Generation, Authentication, RBAC, Forecasting

This script demonstrates:
1. Data ingestion (sensor readings)
2. Admin access to all buildings
3. Owner access to own buildings
4. Owner forbidden from other buildings (RBAC)
5. Forecasting with both roles
6. Recommendations based on thresholds
"""

import requests
import json
import time
from datetime import datetime, timedelta
import sys
import os
import platform

BASE_URL = "http://localhost:8080"

# Detect if we should use colors - disable on Windows by default to avoid ANSI code issues
# Set USE_COLORS=1 environment variable to enable colors if your terminal supports it
# On Windows, disable colors unless explicitly enabled via environment variable
IS_WINDOWS = platform.system() == 'Windows'
if IS_WINDOWS:
    # Windows: Disable colors by default (most terminals don't support ANSI codes)
    USE_COLORS = os.getenv('USE_COLORS', '0') == '1'
else:
    # Unix/Linux: Enable colors if TERM is not 'dumb' or USE_COLORS is set
    USE_COLORS = os.getenv('USE_COLORS', '0') == '1' or os.getenv('TERM') != 'dumb'

# Colors for output (disabled by default on Windows to avoid ANSI code display issues)
class Colors:
    GREEN = '\033[92m' if USE_COLORS else ''
    RED = '\033[91m' if USE_COLORS else ''
    YELLOW = '\033[93m' if USE_COLORS else ''
    BLUE = '\033[94m' if USE_COLORS else ''
    MAGENTA = '\033[95m' if USE_COLORS else ''
    CYAN = '\033[96m' if USE_COLORS else ''
    RESET = '\033[0m' if USE_COLORS else ''
    BOLD = '\033[1m' if USE_COLORS else ''

def print_header(text):
    """Print a formatted header"""
    print(f"\n{Colors.BOLD}{Colors.CYAN}{'=' * 70}{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.CYAN}{text}{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.CYAN}{'=' * 70}{Colors.RESET}\n")

def print_section(text):
    """Print a section header"""
    print(f"\n{Colors.BOLD}{Colors.BLUE}{text}{Colors.RESET}")

def print_success(message):
    """Print success message"""
    print(f"{Colors.GREEN}[PASS] {message}{Colors.RESET}")

def print_error(message):
    """Print error message"""
    print(f"{Colors.RED}[FAIL] {message}{Colors.RESET}")

def print_warning(message):
    """Print warning message"""
    print(f"{Colors.YELLOW}[WARN] {message}{Colors.RESET}")

def print_info(message):
    """Print info message"""
    print(f"{Colors.MAGENTA}[INFO] {message}{Colors.RESET}")

def check_api():
    """Check if API is running"""
    try:
        response = requests.get(f"{BASE_URL}/actuator/health", timeout=2)
        return True
    except requests.exceptions.RequestException:
        return False

def ingest_reading(building_id, sensor_id, timestamp, value):
    """Ingest a sensor reading"""
    url = f"{BASE_URL}/ingest"
    payload = {
        "buildingId": building_id,
        "sensorId": sensor_id,
        "timestamp": timestamp,
        "value": value
    }
    
    try:
        response = requests.post(url, json=payload, timeout=5)
        if response.status_code == 201:
            return True, response.json()
        else:
            return False, f"Status {response.status_code}: {response.text}"
    except requests.exceptions.RequestException as e:
        return False, str(e)

def get_last_readings(building_id, minutes, username, role):
    """Get last readings with authentication"""
    url = f"{BASE_URL}/buildings/{building_id}/last-readings?minutes={minutes}"
    headers = {"Authorization": f"Bearer {username}:{role}"}
    
    try:
        response = requests.get(url, headers=headers, timeout=5)
        if response.status_code == 200:
            return True, response.json()
        elif response.status_code == 403:
            return False, "403 Forbidden - Access Denied"
        else:
            return False, f"Status {response.status_code}: {response.text}"
    except requests.exceptions.RequestException as e:
        return False, str(e)

def get_forecast(building_id, minutes, username, role):
    """Get forecast with authentication"""
    url = f"{BASE_URL}/buildings/{building_id}/forecast?minutes={minutes}"
    headers = {"Authorization": f"Bearer {username}:{role}"}
    
    try:
        response = requests.get(url, headers=headers, timeout=5)
        if response.status_code == 200:
            return True, response.json()
        elif response.status_code == 403:
            return False, "403 Forbidden - Access Denied"
        else:
            return False, f"Status {response.status_code}: {response.text}"
    except requests.exceptions.RequestException as e:
        return False, str(e)

def test_1_data_generation():
    """Test 1: Generate sensor data for all buildings"""
    print_section("TEST 1: Data Generation - Ingesting Sensor Readings")
    print_warning("Note: This test adds new readings. Previous readings in the database are not deleted.")
    print_info("If you run this test multiple times, readings will accumulate in the database.\n")
    
    test_cases = [
        # Building 1 (owned by owner1) - Normal consumption
        {"building": 1, "sensor": "sensor-001", "minutes_ago": 30, "value": 50.5},
        {"building": 1, "sensor": "sensor-001", "minutes_ago": 25, "value": 55.0},
        {"building": 1, "sensor": "sensor-001", "minutes_ago": 20, "value": 60.0},
        {"building": 1, "sensor": "sensor-001", "minutes_ago": 15, "value": 58.0},
        {"building": 1, "sensor": "sensor-001", "minutes_ago": 10, "value": 62.0},
        
        # Building 1 - High consumption readings (for threshold test)
        {"building": 1, "sensor": "sensor-002", "minutes_ago": 5, "value": 120.0},
        {"building": 1, "sensor": "sensor-002", "minutes_ago": 4, "value": 125.0},
        {"building": 1, "sensor": "sensor-002", "minutes_ago": 3, "value": 130.0},
        {"building": 1, "sensor": "sensor-002", "minutes_ago": 2, "value": 128.0},
        {"building": 1, "sensor": "sensor-002", "minutes_ago": 1, "value": 132.0},
        
        # Building 2 (owned by owner1)
        {"building": 2, "sensor": "sensor-003", "minutes_ago": 25, "value": 45.0},
        {"building": 2, "sensor": "sensor-003", "minutes_ago": 20, "value": 48.0},
        {"building": 2, "sensor": "sensor-003", "minutes_ago": 15, "value": 50.0},
        
        # Building 3 (owned by owner2)
        {"building": 3, "sensor": "sensor-004", "minutes_ago": 20, "value": 70.0},
        {"building": 3, "sensor": "sensor-004", "minutes_ago": 15, "value": 72.0},
        {"building": 3, "sensor": "sensor-004", "minutes_ago": 10, "value": 75.0},
    ]
    
    success_count = 0
    total_count = len(test_cases)
    
    for i, test in enumerate(test_cases, 1):
        timestamp = (datetime.now() - timedelta(minutes=test["minutes_ago"])).isoformat()
        success, result = ingest_reading(
            test["building"],
            test["sensor"],
            timestamp,
            test["value"]
        )
        
        if success:
            print_success(f"Reading {i}/{total_count}: Building {test['building']}, Sensor {test['sensor']}, Value: {test['value']}")
            success_count += 1
        else:
            print_error(f"Reading {i}/{total_count}: Failed - {result}")
    
    print_info(f"\nData Generation Summary: {success_count}/{total_count} readings ingested successfully")
    return success_count == total_count

def test_2_admin_access():
    """Test 2: Admin can access all buildings"""
    print_section("TEST 2: Admin Access - Admin Can Access All Buildings")
    print_info("This test retrieves ALL readings from the last 60 minutes (may include readings from previous test runs).\n")
    
    buildings = [1, 2, 3]
    all_passed = True
    
    for building_id in buildings:
        success, result = get_last_readings(building_id, 60, "admin", "ADMIN")
        
        if success:
            readings_count = len(result) if isinstance(result, list) else 0
            print_success(f"Admin accessed Building {building_id}: {readings_count} readings retrieved (from last 60 minutes)")
        else:
            print_error(f"Admin failed to access Building {building_id}: {result}")
            all_passed = False
    
    # Test forecast access
    success, result = get_forecast(1, 60, "admin", "ADMIN")
    if success:
        forecast_points = len(result.get("forecast", []))
        recommendation = result.get("recommendation", "N/A")
        print_success(f"Admin accessed forecast for Building 1: {forecast_points} forecast points")
        print_info(f"Recommendation: {recommendation[:80]}...")
    else:
        print_error(f"Admin failed to get forecast for Building 1: {result}")
        all_passed = False
    
    return all_passed

def test_3_owner_access_own_buildings():
    """Test 3: Owner can access their own buildings"""
    print_section("TEST 3: Owner Access - Owner Can Access Own Buildings")
    
    # owner1 owns Building 1 and 2
    owner1_buildings = [1, 2]
    all_passed = True
    
    for building_id in owner1_buildings:
        success, result = get_last_readings(building_id, 60, "owner1", "OWNER")
        
        if success:
            readings_count = len(result) if isinstance(result, list) else 0
            print_success(f"Owner1 accessed Building {building_id}: {readings_count} readings retrieved")
        else:
            print_error(f"Owner1 failed to access Building {building_id}: {result}")
            all_passed = False
    
    # Test forecast access
    success, result = get_forecast(1, 60, "owner1", "OWNER")
    if success:
        forecast_points = len(result.get("forecast", []))
        recommendation = result.get("recommendation", "N/A")
        print_success(f"Owner1 accessed forecast for Building 1: {forecast_points} forecast points")
        print_info(f"Recommendation: {recommendation[:80]}...")
    else:
        print_error(f"Owner1 failed to get forecast for Building 1: {result}")
        all_passed = False
    
    # owner2 owns Building 3
    success, result = get_last_readings(3, 60, "owner2", "OWNER")
    if success:
        readings_count = len(result) if isinstance(result, list) else 0
        print_success(f"Owner2 accessed Building 3: {readings_count} readings retrieved")
    else:
        print_error(f"Owner2 failed to access Building 3: {result}")
        all_passed = False
    
    return all_passed

def test_4_owner_forbidden_from_others():
    """Test 4: Owner cannot access other owners' buildings (RBAC)"""
    print_section("TEST 4: RBAC Enforcement - Owner Cannot Access Other Buildings")
    print_info("This test demonstrates RBAC: Owners can ONLY access their own buildings.\n")
    
    all_passed = True
    
    # First, demonstrate that owner1 CAN access their own buildings
    print_info("Step 1: Verifying owner1 CAN access their own buildings...")
    success, result = get_last_readings(1, 60, "owner1", "OWNER")
    if success:
        readings_count = len(result) if isinstance(result, list) else 0
        print_success(f"Owner1 successfully accessed Building 1 (their own): {readings_count} readings")
    else:
        print_error(f"Owner1 should access Building 1, but failed: {result}")
        all_passed = False
    
    print()
    print_info("Step 2: Demonstrating that owner1 CANNOT access other owners' buildings...")
    
    # owner1 tries to access Building 3 (owned by owner2) - should fail
    print_warning("Attempting: Owner1 trying to access Building 3 (owned by owner2)...")
    success, result = get_last_readings(3, 60, "owner1", "OWNER")
    if not success and ("403" in str(result) or "Forbidden" in str(result)):
        print_success(f"RBAC Enforced: Owner1 correctly DENIED access to Building 3 (403 Forbidden)")
        print_info("  -> This is correct behavior! Owners cannot access buildings they don't own.")
    else:
        print_error(f"RBAC Failed: Owner1 should be denied access to Building 3, but got: {result}")
        all_passed = False
    
    print()
    
    # owner2 tries to access Building 1 (owned by owner1) - should fail
    print_warning("Attempting: Owner2 trying to access Building 1 (owned by owner1)...")
    success, result = get_last_readings(1, 60, "owner2", "OWNER")
    if not success and ("403" in str(result) or "Forbidden" in str(result)):
        print_success(f"RBAC Enforced: Owner2 correctly DENIED access to Building 1 (403 Forbidden)")
        print_info("  -> This is correct behavior! Owners cannot access buildings they don't own.")
    else:
        print_error(f"RBAC Failed: Owner2 should be denied access to Building 1, but got: {result}")
        all_passed = False
    
    print()
    
    # Test forecast access forbidden
    print_warning("Attempting: Owner1 trying to get forecast for Building 3 (owned by owner2)...")
    success, result = get_forecast(3, 60, "owner1", "OWNER")
    if not success and ("403" in str(result) or "Forbidden" in str(result)):
        print_success(f"RBAC Enforced: Owner1 correctly DENIED forecast access to Building 3 (403 Forbidden)")
        print_info("  -> Forecast access is also protected by RBAC!")
    else:
        print_error(f"RBAC Failed: Owner1 should be denied forecast access to Building 3, but got: {result}")
        all_passed = False
    
    print()
    print_info("RBAC Summary: Owners can only access their own buildings. Access to other buildings is correctly denied!")
    
    return all_passed

def test_5_forecasting_normal():
    """Test 5: Forecasting with normal consumption"""
    print_section("TEST 5: Forecasting - Normal Consumption (No Alert)")
    
    # Building 2 has normal readings (45, 48, 50) - average = 47.67 < 100
    success, result = get_forecast(2, 60, "owner1", "OWNER")
    
    if success:
        forecast = result.get("forecast", [])
        recommendation = result.get("recommendation", "")
        
        if forecast:
            avg_value = sum(point.get("value", 0) for point in forecast[:5]) / min(5, len(forecast))
            print_success(f"Forecast generated for Building 2: {len(forecast)} points, avg ~{avg_value:.1f}")
        else:
            print_warning("No forecast points generated (insufficient data)")
        
        if "No action required" in recommendation or "normal range" in recommendation.lower():
            print_success(f"Recommendation correct: Normal consumption - no action needed")
            print_info(f"Full recommendation: {recommendation}")
            return True
        else:
            print_error(f"Unexpected recommendation for normal consumption: {recommendation}")
            return False
    else:
        print_error(f"Failed to get forecast: {result}")
        return False

def test_6_forecasting_high():
    """Test 6: Forecasting with high consumption (Alert)"""
    print_section("TEST 6: Forecasting - High Consumption (Alert Threshold Exceeded)")
    
    # Building 1 has high readings (120, 125, 130, 128, 132) - average = 127 > 100
    success, result = get_forecast(1, 60, "admin", "ADMIN")
    
    if success:
        forecast = result.get("forecast", [])
        recommendation = result.get("recommendation", "")
        
        if forecast:
            avg_value = sum(point.get("value", 0) for point in forecast[:5]) / min(5, len(forecast))
            print_success(f"Forecast generated for Building 1: {len(forecast)} points, avg ~{avg_value:.1f}")
        else:
            print_warning("No forecast points generated (insufficient data)")
        
        if "energy-saving" in recommendation.lower() or "exceeds threshold" in recommendation.lower():
            print_success(f"Recommendation correct: High consumption detected - action recommended")
            print_info(f"Full recommendation: {recommendation}")
            return True
        else:
            print_error(f"Unexpected recommendation for high consumption: {recommendation}")
            return False
    else:
        print_error(f"Failed to get forecast: {result}")
        return False

def test_7_authentication():
    """Test 7: Authentication without token should fail"""
    print_section("TEST 7: Authentication - Requests Without Token Should Fail")
    
    all_passed = True
    
    # Try to access without authentication
    try:
        response = requests.get(f"{BASE_URL}/buildings/1/last-readings?minutes=60", timeout=5)
        if response.status_code == 401 or response.status_code == 403:
            print_success(f"Authentication enforced: Request without token correctly rejected ({response.status_code})")
        else:
            print_error(f"Authentication failed: Expected 401/403, got {response.status_code}")
            all_passed = False
    except requests.exceptions.RequestException as e:
        print_error(f"Authentication test failed: {e}")
        all_passed = False
    
    return all_passed

def wait_for_keypress(prompt="Press Enter to continue..."):
    """Wait for user to press Enter"""
    try:
        input(f"\n{Colors.YELLOW}{prompt}{Colors.RESET}")
    except KeyboardInterrupt:
        print(f"\n{Colors.YELLOW}Test interrupted by user{Colors.RESET}")
        sys.exit(0)

def show_menu():
    """Display test menu"""
    print_header("DECODER API - Comprehensive Test Suite")
    print(f"{Colors.BOLD}Select a test to run:{Colors.RESET}\n")
    print(f"  {Colors.CYAN}1{Colors.RESET} - Data Generation (Ingest Sensor Readings)")
    print(f"  {Colors.CYAN}2{Colors.RESET} - Admin Access (Access All Buildings)")
    print(f"  {Colors.CYAN}3{Colors.RESET} - Owner Access (Own Buildings)")
    print(f"  {Colors.CYAN}4{Colors.RESET} - RBAC Enforcement (Unauthorized Access)")
    print(f"  {Colors.CYAN}5{Colors.RESET} - Forecasting - Normal Consumption")
    print(f"  {Colors.CYAN}6{Colors.RESET} - Forecasting - High Consumption (Alert)")
    print(f"  {Colors.CYAN}7{Colors.RESET} - Authentication Enforcement")
    print(f"  {Colors.CYAN}8{Colors.RESET} - Run All Tests")
    print(f"  {Colors.CYAN}0{Colors.RESET} - Exit\n")

def run_interactive_test(test_func, test_name):
    """Run a single test interactively"""
    print(f"\n{Colors.BOLD}{Colors.MAGENTA}{'='*70}{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.MAGENTA}Running: {test_name}{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.MAGENTA}{'='*70}{Colors.RESET}\n")
    
    try:
        result = test_func()
        
        if result:
            print(f"\n{Colors.GREEN}{Colors.BOLD}[PASS] Test PASSED{Colors.RESET}")
        else:
            print(f"\n{Colors.RED}{Colors.BOLD}[FAIL] Test FAILED{Colors.RESET}")
        
        wait_for_keypress("Press Enter to continue...")
        return result
    except KeyboardInterrupt:
        print(f"\n{Colors.YELLOW}Test interrupted by user{Colors.RESET}")
        return None

def main():
    """Run interactive test suite"""
    print_header("DECODER API - Interactive Test Suite")
    print_info("Testing: Data Generation, Authentication, RBAC, Forecasting")
    print_info(f"Target API: {BASE_URL}\n")
    
    # Check if API is running
    print_section("Pre-flight Check")
    if not check_api():
        print_error("API is not running at " + BASE_URL)
        print_warning("Please start the API first: mvn spring-boot:run")
        sys.exit(1)
    print_success("API is running")
    
    wait_for_keypress("Press Enter to start...")
    
    # Test results storage
    results = {}
    
    while True:
        show_menu()
        
        try:
            choice = input(f"{Colors.BOLD}Enter your choice (0-8): {Colors.RESET}").strip()
            
            if choice == "0":
                print(f"\n{Colors.YELLOW}Exiting test suite...{Colors.RESET}")
                break
            
            elif choice == "1":
                results["Data Generation"] = run_interactive_test(
                    test_1_data_generation, 
                    "TEST 1: Data Generation - Ingesting Sensor Readings"
                )
                time.sleep(0.5)  # Brief pause
            
            elif choice == "2":
                results["Admin Access"] = run_interactive_test(
                    test_2_admin_access,
                    "TEST 2: Admin Access - Admin Can Access All Buildings"
                )
            
            elif choice == "3":
                results["Owner Access (Own Buildings)"] = run_interactive_test(
                    test_3_owner_access_own_buildings,
                    "TEST 3: Owner Access - Owner Can Access Own Buildings"
                )
            
            elif choice == "4":
                results["RBAC Enforcement"] = run_interactive_test(
                    test_4_owner_forbidden_from_others,
                    "TEST 4: RBAC Enforcement - Owner Cannot Access Other Buildings"
                )
            
            elif choice == "5":
                results["Forecasting (Normal)"] = run_interactive_test(
                    test_5_forecasting_normal,
                    "TEST 5: Forecasting - Normal Consumption (No Alert)"
                )
            
            elif choice == "6":
                results["Forecasting (High)"] = run_interactive_test(
                    test_6_forecasting_high,
                    "TEST 6: Forecasting - High Consumption (Alert Threshold Exceeded)"
                )
            
            elif choice == "7":
                results["Authentication"] = run_interactive_test(
                    test_7_authentication,
                    "TEST 7: Authentication - Requests Without Token Should Fail"
                )
            
            elif choice == "8":
                # Run all tests sequentially
                print_header("Running All Tests Sequentially")
                print_warning("All tests will run one after another. Press Enter after each test.\n")
                
                if "Data Generation" not in results:
                    results["Data Generation"] = run_interactive_test(
                        test_1_data_generation,
                        "TEST 1: Data Generation - Ingesting Sensor Readings"
                    )
                    time.sleep(1)
                
                results["Admin Access"] = run_interactive_test(
                    test_2_admin_access,
                    "TEST 2: Admin Access - Admin Can Access All Buildings"
                )
                
                results["Owner Access (Own Buildings)"] = run_interactive_test(
                    test_3_owner_access_own_buildings,
                    "TEST 3: Owner Access - Owner Can Access Own Buildings"
                )
                
                results["RBAC Enforcement"] = run_interactive_test(
                    test_4_owner_forbidden_from_others,
                    "TEST 4: RBAC Enforcement - Owner Cannot Access Other Buildings"
                )
                
                results["Forecasting (Normal)"] = run_interactive_test(
                    test_5_forecasting_normal,
                    "TEST 5: Forecasting - Normal Consumption (No Alert)"
                )
                
                results["Forecasting (High)"] = run_interactive_test(
                    test_6_forecasting_high,
                    "TEST 6: Forecasting - High Consumption (Alert Threshold Exceeded)"
                )
                
                results["Authentication"] = run_interactive_test(
                    test_7_authentication,
                    "TEST 7: Authentication - Requests Without Token Should Fail"
                )
                
                # Print final summary
                print_header("Final Test Summary")
                
                passed = sum(1 for v in results.values() if v)
                total = len(results)
                
                for test_name, result in results.items():
                    status = f"{Colors.GREEN}PASS{Colors.RESET}" if result else f"{Colors.RED}FAIL{Colors.RESET}"
                    print(f"{status}: {test_name}")
                
                print(f"\n{Colors.BOLD}Total: {passed}/{total} tests passed{Colors.RESET}")
                
                if passed == total:
                    print(f"\n{Colors.GREEN}{Colors.BOLD}All tests passed!{Colors.RESET}")
                else:
                    print(f"\n{Colors.RED}{Colors.BOLD}[WARN] Some tests failed{Colors.RESET}")
                
                wait_for_keypress("Press Enter to return to menu...")
            
            else:
                print_error("Invalid choice. Please enter a number between 0 and 8.")
                wait_for_keypress()
        
        except KeyboardInterrupt:
            print(f"\n\n{Colors.YELLOW}Test suite interrupted by user{Colors.RESET}")
            break
        except Exception as e:
            print_error(f"An error occurred: {e}")
            wait_for_keypress("Press Enter to continue...")
    
    print(f"\n{Colors.CYAN}Thank you for using the DECODER API Test Suite!{Colors.RESET}\n")

if __name__ == "__main__":
    main()

