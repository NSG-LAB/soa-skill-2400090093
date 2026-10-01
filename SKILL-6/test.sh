#!/usr/bin/env bash

BASE_URL="http://localhost:8086/books"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE} Skill Experiment 6 — Library Management Microservice ${NC}"
echo -e "${BLUE}======================================================${NC}"

# Check server connectivity
if ! curl -s --connect-timeout 2 "http://localhost:8086/books" > /dev/null; then
    echo -e "${RED}[ERROR] Service is not running on http://localhost:8086${NC}"
    echo "Please start the service with: mvn spring-boot:run"
    exit 1
fi

echo -e "\n${YELLOW}--- SECTION A: POSITIVE CRUD TESTS ---${NC}"

echo -e "\n${GREEN}1. POST /books — Create Book${NC}"
CREATE_RES=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "9780132350884"
  }')
echo "$CREATE_RES" | grep -v "HTTP_STATUS"
BOOK_ID=$(echo "$CREATE_RES" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
STATUS=$(echo "$CREATE_RES" | grep "HTTP_STATUS" | cut -d':' -f2)
echo -e "Status: ${GREEN}$STATUS${NC} | Created Book ID: ${GREEN}$BOOK_ID${NC}"

echo -e "\n${GREEN}2. GET /books — Retrieve All Books${NC}"
curl -s -i "$BASE_URL"

echo -e "\n\n${GREEN}3. GET /books/$BOOK_ID — Retrieve Book by ID${NC}"
curl -s -i "$BASE_URL/$BOOK_ID"

echo -e "\n\n${GREEN}4. PUT /books/$BOOK_ID — Update Book Details${NC}"
curl -s -i -X PUT "$BASE_URL/$BOOK_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code: A Handbook of Agile Software Craftsmanship",
    "author": "Uncle Bob Martin",
    "isbn": "9780132350884"
  }'

echo -e "\n\n${GREEN}5. GET /books/$BOOK_ID — Verify Persistence of Update${NC}"
curl -s -i "$BASE_URL/$BOOK_ID"

echo -e "\n\n${GREEN}6. DELETE /books/$BOOK_ID — Delete Book${NC}"
curl -s -i -X DELETE "$BASE_URL/$BOOK_ID"

echo -e "\n\n${GREEN}7. GET /books/$BOOK_ID — Confirm Deleted Book Returns 404${NC}"
curl -s -i "$BASE_URL/$BOOK_ID"

echo -e "\n\n${YELLOW}--- SECTION B: NEGATIVE & VALIDATION TESTS ---${NC}"

echo -e "\n${GREEN}8. POST /books — Blank Title (Expected: 400 Bad Request)${NC}"
curl -s -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "",
    "author": "Robert C. Martin",
    "isbn": "9780132350884"
  }'

echo -e "\n\n${GREEN}9. POST /books — Missing Author (Expected: 400 Bad Request)${NC}"
curl -s -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "author": "",
    "isbn": "9780132350884"
  }'

echo -e "\n\n${GREEN}10. POST /books — Invalid ISBN Format (Expected: 400 Bad Request)${NC}"
curl -s -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "invalid-isbn-123"
  }'

echo -e "\n\n${GREEN}11. POST /books — Malformed JSON (Expected: 400 Bad Request)${NC}"
curl -s -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{ "title": "Clean Code", "author": '

echo -e "\n\n${GREEN}12. POST /books — Duplicate ISBN (Expected: 409 Conflict)${NC}"
# First create a reference book
curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Refactoring",
    "author": "Martin Fowler",
    "isbn": "9780201485677"
  }' > /dev/null

# Try creating duplicate
curl -s -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Refactoring Second Edition",
    "author": "Martin Fowler",
    "isbn": "9780201485677"
  }'

echo -e "\n\n${GREEN}13. GET /books/999999 — Nonexistent Book (Expected: 404 Not Found)${NC}"
curl -s -i "$BASE_URL/999999"

echo -e "\n\n${GREEN}14. PUT /books/999999 — Nonexistent Book (Expected: 404 Not Found)${NC}"
curl -s -i -X PUT "$BASE_URL/999999" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Nonexistent",
    "author": "Nobody",
    "isbn": "9780132350884"
  }'

echo -e "\n\n${GREEN}15. DELETE /books/999999 — Nonexistent Book (Expected: 404 Not Found)${NC}"
curl -s -i -X DELETE "$BASE_URL/999999"

echo -e "\n\n${GREEN}16. GET /books/invalid-id — Invalid ID Type (Expected: 400 Bad Request)${NC}"
curl -s -i "$BASE_URL/not-a-number"

echo -e "\n\n${BLUE}======================================================${NC}"
echo -e "${BLUE} All Automated Verification Checks Completed!        ${NC}"
echo -e "${BLUE}======================================================${NC}"

