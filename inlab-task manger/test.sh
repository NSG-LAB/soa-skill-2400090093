#!/usr/bin/env bash

# ==============================================================================
# In-Lab SOA Microservice: Task Manager REST API Test Suite
# Presentation Script for Classroom Demonstration
# ==============================================================================

BASE_URL="http://localhost:8090/api/tasks"

# ANSI Color Codes
GREEN='\033[1;32m'
BLUE='\033[1;34m'
YELLOW='\033[1;33m'
RED='\033[1;31m'
CYAN='\033[1;36m'
MAGENTA='\033[1;35m'
BOLD='\033[1m'
NC='\033[0m' # No Color

clear

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BOLD}${CYAN}   🚀 SOA IN-LAB TASK MANAGER MICROSERVICE — LIVE TEST SUITE   ${NC}"
echo -e "${BLUE}======================================================================${NC}"
echo -e "Base Endpoint: ${MAGENTA}${BASE_URL}${NC}"
echo -e "Testing Architecture: Spring Boot 3 + Spring Data JPA + MySQL"
echo -e "${BLUE}----------------------------------------------------------------------${NC}"

# Check server connectivity
if ! curl -s --connect-timeout 2 "$BASE_URL/summary" > /dev/null; then
    echo -e "${RED}[ERROR] Service is not responding on http://localhost:8090${NC}"
    echo -e "Please start the backend service first using:"
    echo -e "  ${YELLOW}mvn spring-boot:run${NC}"
    exit 1
fi

TOTAL_TESTS=0
PASSED_TESTS=0

run_test() {
    local test_num="$1"
    local method="$2"
    local endpoint="$3"
    local description="$4"
    local expected_status="$5"
    local actual_status="$6"
    local response="$7"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "\n${BOLD}${CYAN}[TEST $test_num] ${YELLOW}${method} ${endpoint}${NC}"
    echo -e "  ${BOLD}Description:${NC} $description"

    if [ "$actual_status" -eq "$expected_status" ]; then
        echo -e "  ${BOLD}Result:${NC}      ${GREEN}✔ PASSED${NC} (Status: ${GREEN}$actual_status${NC} | Expected: $expected_status)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "  ${BOLD}Result:${NC}      ${RED}✘ FAILED${NC} (Status: ${RED}$actual_status${NC} | Expected: $expected_status)"
    fi

    echo -e "  ${BOLD}Payload Response:${NC}"
    if [ -z "$response" ]; then
        echo -e "    ${MAGENTA}(No Content — HTTP 204)${NC}"
    elif command -v jq >/dev/null 2>&1 && echo "$response" | jq . >/dev/null 2>&1; then
        echo "$response" | jq -C '.' | sed 's/^/    /'
    else
        echo -e "    ${response}"
    fi
}

echo -e "\n${YELLOW}======================================================================${NC}"
echo -e "${YELLOW}  SECTION 1: CORE CRUD OPERATIONS & LIFECYCLE MANAGEMENT             ${NC}"
echo -e "${YELLOW}======================================================================${NC}"

# 1. POST /api/tasks — Create Task
CREATE_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Classroom Demo Task",
    "description": "Demonstrating RESTful SOA endpoints in lab session",
    "status": "TODO",
    "priority": "HIGH",
    "category": "Academics",
    "dueDate": "2026-10-31"
  }')

CREATE_STATUS=$(echo "$CREATE_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
CREATE_BODY=$(echo "$CREATE_RAW" | sed '/HTTP_STATUS/d')
TASK_ID=$(echo "$CREATE_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

run_test "1" "POST" "/api/tasks" "Create a new task with validation" 201 "$CREATE_STATUS" "$CREATE_BODY"

# 2. GET /api/tasks — List All
GET_ALL_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL")
GET_ALL_STATUS=$(echo "$GET_ALL_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
GET_ALL_BODY=$(echo "$GET_ALL_RAW" | sed '/HTTP_STATUS/d')
run_test "2" "GET" "/api/tasks" "Retrieve all tasks from database" 200 "$GET_ALL_STATUS" "$GET_ALL_BODY"

# 3. GET /api/tasks/{id} — Single Item by ID
GET_ID_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/$TASK_ID")
GET_ID_STATUS=$(echo "$GET_ID_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
GET_ID_BODY=$(echo "$GET_ID_RAW" | sed '/HTTP_STATUS/d')
run_test "3" "GET" "/api/tasks/$TASK_ID" "Fetch single task by dynamically created ID" 200 "$GET_ID_STATUS" "$GET_ID_BODY"

# 4. PUT /api/tasks/{id} — Full Update
PUT_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X PUT "$BASE_URL/$TASK_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Classroom Demo Task (Updated)",
    "description": "Full entity update via HTTP PUT",
    "status": "IN_PROGRESS",
    "priority": "URGENT",
    "category": "Academics",
    "dueDate": "2026-11-15"
  }')
PUT_STATUS=$(echo "$PUT_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
PUT_BODY=$(echo "$PUT_RAW" | sed '/HTTP_STATUS/d')
run_test "4" "PUT" "/api/tasks/$TASK_ID" "Perform full idempotent entity update" 200 "$PUT_STATUS" "$PUT_BODY"

# 5. PATCH /api/tasks/{id}/status — Lightweight Transition
PATCH_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X PATCH "$BASE_URL/$TASK_ID/status" \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}')
PATCH_STATUS=$(echo "$PATCH_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
PATCH_BODY=$(echo "$PATCH_RAW" | sed '/HTTP_STATUS/d')
run_test "5" "PATCH" "/api/tasks/$TASK_ID/status" "Lightweight state transition to COMPLETED" 200 "$PATCH_STATUS" "$PATCH_BODY"

echo -e "\n${YELLOW}======================================================================${NC}"
echo -e "${YELLOW}  SECTION 2: JPA QUERIES, METRICS & FILTERING                       ${NC}"
echo -e "${YELLOW}======================================================================${NC}"

# 6. GET /api/tasks/summary — Real-time Statistics
SUMMARY_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/summary")
SUMMARY_STATUS=$(echo "$SUMMARY_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
SUMMARY_BODY=$(echo "$SUMMARY_RAW" | sed '/HTTP_STATUS/d')
run_test "6" "GET" "/api/tasks/summary" "Retrieve aggregated statistics & counts" 200 "$SUMMARY_STATUS" "$SUMMARY_BODY"

# 7. GET /api/tasks?status=COMPLETED — Filter by Enum Status
FILTER_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL?status=COMPLETED")
FILTER_STATUS=$(echo "$FILTER_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
FILTER_BODY=$(echo "$FILTER_RAW" | sed '/HTTP_STATUS/d')
run_test "7" "GET" "/api/tasks?status=COMPLETED" "JPA filter query by task status" 200 "$FILTER_STATUS" "$FILTER_BODY"

# 8. GET /api/tasks?search=Updated — Substring Search
SEARCH_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL?search=Updated")
SEARCH_STATUS=$(echo "$SEARCH_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
SEARCH_BODY=$(echo "$SEARCH_RAW" | sed '/HTTP_STATUS/d')
run_test "8" "GET" "/api/tasks?search=Updated" "Custom JPA search query across title & description" 200 "$SEARCH_STATUS" "$SEARCH_BODY"

echo -e "\n${YELLOW}======================================================================${NC}"
echo -e "${YELLOW}  SECTION 3: ROBUST ERROR HANDLING & BEAN VALIDATION                ${NC}"
echo -e "${YELLOW}======================================================================${NC}"

# 9. POST /api/tasks with Blank Title
INVALID_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{"title": "", "status": "TODO"}')
INVALID_STATUS=$(echo "$INVALID_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
INVALID_BODY=$(echo "$INVALID_RAW" | sed '/HTTP_STATUS/d')
run_test "9" "POST" "/api/tasks (Empty Title)" "Validation failure with blank title (Expect 400)" 400 "$INVALID_STATUS" "$INVALID_BODY"

# 10. GET /api/tasks/999999 (Non-existent ID)
NOT_FOUND_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/999999")
NOT_FOUND_STATUS=$(echo "$NOT_FOUND_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
NOT_FOUND_BODY=$(echo "$NOT_FOUND_RAW" | sed '/HTTP_STATUS/d')
run_test "10" "GET" "/api/tasks/999999" "ResourceNotFoundException mapped to 404 Not Found" 404 "$NOT_FOUND_STATUS" "$NOT_FOUND_BODY"

echo -e "\n${YELLOW}======================================================================${NC}"
echo -e "${YELLOW}  SECTION 4: RESOURCE DELETION & POST-DELETE CONSISTENCY            ${NC}"
echo -e "${YELLOW}======================================================================${NC}"

# 11. DELETE /api/tasks/{id}
DELETE_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X DELETE "$BASE_URL/$TASK_ID")
DELETE_STATUS=$(echo "$DELETE_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
DELETE_BODY=$(echo "$DELETE_RAW" | sed '/HTTP_STATUS/d')
run_test "11" "DELETE" "/api/tasks/$TASK_ID" "Delete task permanently from database (Expect 204)" 204 "$DELETE_STATUS" "$DELETE_BODY"

# 12. GET /api/tasks/{id} after deletion
VERIFY_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/$TASK_ID")
VERIFY_STATUS=$(echo "$VERIFY_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
VERIFY_BODY=$(echo "$VERIFY_RAW" | sed '/HTTP_STATUS/d')
run_test "12" "GET" "/api/tasks/$TASK_ID" "Verify entity is gone (Expect 404)" 404 "$VERIFY_STATUS" "$VERIFY_BODY"

echo -e "\n${BLUE}======================================================================${NC}"
echo -e "${BOLD}${BLUE}  SUMMARY: ${GREEN}${PASSED_TESTS}/${TOTAL_TESTS} TESTS PASSED${NC}"
echo -e "${BLUE}======================================================================${NC}"

if [ "$PASSED_TESTS" -eq "$TOTAL_TESTS" ]; then
    echo -e "${GREEN}${BOLD}✔ ALL 12 TEST CASES EXECUTED AND PASSED SUCCESSFULLY!${NC}\n"
    exit 0
else
    echo -e "${RED}${BOLD}✘ SOME TESTS FAILED.${NC}\n"
    exit 1
fi
