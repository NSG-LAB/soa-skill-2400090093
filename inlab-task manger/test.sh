#!/usr/bin/env bash

BASE_URL="http://localhost:8090/api/tasks"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}   In-Lab Task Manager Microservice — REST API Tests  ${NC}"
echo -e "${BLUE}======================================================${NC}"

# Check server connectivity
if ! curl -s --connect-timeout 2 "$BASE_URL" > /dev/null; then
    echo -e "${RED}[ERROR] Service is not running on http://localhost:8090${NC}"
    echo "Please start the service with: mvn spring-boot:run"
    exit 1
fi

TOTAL_TESTS=0
PASSED_TESTS=0

run_test() {
    local test_name="$1"
    local expected_status="$2"
    local actual_status="$3"
    local response="$4"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "\n${CYAN}Test $TOTAL_TESTS: $test_name${NC}"
    if [ "$actual_status" -eq "$expected_status" ]; then
        echo -e "${GREEN}[PASS] Status: $actual_status (Expected: $expected_status)${NC}"
        echo -e "Response: ${response:0:180}..."
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}[FAIL] Status: $actual_status (Expected: $expected_status)${NC}"
        echo -e "Response: $response"
    fi
}

echo -e "\n${YELLOW}--- SECTION A: POSITIVE CRUD & FILTER TESTS ---${NC}"

# 1. POST /api/tasks — Create Task
CREATE_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Automated Test Task",
    "description": "Verifying microservice REST API contract",
    "status": "TODO",
    "priority": "HIGH",
    "category": "Testing",
    "dueDate": "2026-12-31"
  }')

CREATE_STATUS=$(echo "$CREATE_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
CREATE_BODY=$(echo "$CREATE_RAW" | sed '/HTTP_STATUS/d')
TASK_ID=$(echo "$CREATE_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
run_test "POST /api/tasks — Create Task" 201 "$CREATE_STATUS" "$CREATE_BODY"

# 2. GET /api/tasks — List Tasks
GET_ALL_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL")
GET_ALL_STATUS=$(echo "$GET_ALL_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
GET_ALL_BODY=$(echo "$GET_ALL_RAW" | sed '/HTTP_STATUS/d')
run_test "GET /api/tasks — List All Tasks" 200 "$GET_ALL_STATUS" "$GET_ALL_BODY"

# 3. GET /api/tasks/{id} — Retrieve by ID
GET_ID_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/$TASK_ID")
GET_ID_STATUS=$(echo "$GET_ID_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
GET_ID_BODY=$(echo "$GET_ID_RAW" | sed '/HTTP_STATUS/d')
run_test "GET /api/tasks/$TASK_ID — Get Task by ID" 200 "$GET_ID_STATUS" "$GET_ID_BODY"

# 4. PUT /api/tasks/{id} — Update Task
UPDATE_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X PUT "$BASE_URL/$TASK_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Automated Test Task (Updated)",
    "description": "Updated description with verified payload",
    "status": "IN_PROGRESS",
    "priority": "URGENT",
    "category": "QA",
    "dueDate": "2026-12-31"
  }')
UPDATE_STATUS=$(echo "$UPDATE_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
UPDATE_BODY=$(echo "$UPDATE_RAW" | sed '/HTTP_STATUS/d')
run_test "PUT /api/tasks/$TASK_ID — Update Task" 200 "$UPDATE_STATUS" "$UPDATE_BODY"

# 5. PATCH /api/tasks/{id}/status — Status transition to COMPLETED
PATCH_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X PATCH "$BASE_URL/$TASK_ID/status" \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}')
PATCH_STATUS=$(echo "$PATCH_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
PATCH_BODY=$(echo "$PATCH_RAW" | sed '/HTTP_STATUS/d')
run_test "PATCH /api/tasks/$TASK_ID/status — Transition to COMPLETED" 200 "$PATCH_STATUS" "$PATCH_BODY"

# 6. GET /api/tasks/summary — Dashboard Metrics
SUMMARY_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/summary")
SUMMARY_STATUS=$(echo "$SUMMARY_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
SUMMARY_BODY=$(echo "$SUMMARY_RAW" | sed '/HTTP_STATUS/d')
run_test "GET /api/tasks/summary — Get Metric Summary" 200 "$SUMMARY_STATUS" "$SUMMARY_BODY"

# 7. GET /api/tasks?status=COMPLETED — Filter by Status
FILTER_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL?status=COMPLETED")
FILTER_STATUS=$(echo "$FILTER_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
FILTER_BODY=$(echo "$FILTER_RAW" | sed '/HTTP_STATUS/d')
run_test "GET /api/tasks?status=COMPLETED — Filter by Status" 200 "$FILTER_STATUS" "$FILTER_BODY"

# 8. GET /api/tasks?search=Updated — Keyword Search
SEARCH_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL?search=Updated")
SEARCH_STATUS=$(echo "$SEARCH_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
SEARCH_BODY=$(echo "$SEARCH_RAW" | sed '/HTTP_STATUS/d')
run_test "GET /api/tasks?search=Updated — Search by Keyword" 200 "$SEARCH_STATUS" "$SEARCH_BODY"

echo -e "\n${YELLOW}--- SECTION B: NEGATIVE & VALIDATION TESTS ---${NC}"

# 9. POST /api/tasks with blank title (Validation failure)
INV_POST_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{"title": "", "status": "TODO"}')
INV_POST_STATUS=$(echo "$INV_POST_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
INV_POST_BODY=$(echo "$INV_POST_RAW" | sed '/HTTP_STATUS/d')
run_test "POST /api/tasks with Blank Title (Validation Expect 400)" 400 "$INV_POST_STATUS" "$INV_POST_BODY"

# 10. GET /api/tasks/999999 (Non-existent task)
NOT_FOUND_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/999999")
NOT_FOUND_STATUS=$(echo "$NOT_FOUND_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
NOT_FOUND_BODY=$(echo "$NOT_FOUND_RAW" | sed '/HTTP_STATUS/d')
run_test "GET /api/tasks/999999 (Expect 404 Not Found)" 404 "$NOT_FOUND_STATUS" "$NOT_FOUND_BODY"

echo -e "\n${YELLOW}--- SECTION C: DELETE & VERIFY ---${NC}"

# 11. DELETE /api/tasks/{id}
DEL_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X DELETE "$BASE_URL/$TASK_ID")
DEL_STATUS=$(echo "$DEL_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
DEL_BODY=$(echo "$DEL_RAW" | sed '/HTTP_STATUS/d')
run_test "DELETE /api/tasks/$TASK_ID — Delete Task" 204 "$DEL_STATUS" "$DEL_BODY"

# 12. GET /api/tasks/{id} after delete (Expect 404)
VERIFY_RAW=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$BASE_URL/$TASK_ID")
VERIFY_STATUS=$(echo "$VERIFY_RAW" | grep "HTTP_STATUS" | cut -d':' -f2)
VERIFY_BODY=$(echo "$VERIFY_RAW" | sed '/HTTP_STATUS/d')
run_test "GET /api/tasks/$TASK_ID after delete (Expect 404)" 404 "$VERIFY_STATUS" "$VERIFY_BODY"

echo -e "\n${BLUE}======================================================${NC}"
echo -e "${BLUE}  Test Results: ${PASSED_TESTS}/${TOTAL_TESTS} Passed ${NC}"
echo -e "${BLUE}======================================================${NC}"

if [ "$PASSED_TESTS" -eq "$TOTAL_TESTS" ]; then
    echo -e "${GREEN}All tests passed successfully!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed.${NC}"
    exit 1
fi
