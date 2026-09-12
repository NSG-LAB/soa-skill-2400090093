#!/usr/bin/env bash

BASE_URL="http://localhost:8086/books"

echo "=========================================="
echo " Testing Library CRUD APIs"
echo "=========================================="

echo ""
echo "1. ADD BOOK"
curl -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "isbn": "9780132350884"
  }'

echo ""
echo ""
echo "2. VIEW ALL BOOKS"
curl -i "$BASE_URL"

echo ""
echo ""
echo "3. VIEW BOOK ID 1"
curl -i "$BASE_URL/1"

echo ""
echo ""
echo "4. UPDATE BOOK ID 1"
curl -i -X PUT "$BASE_URL/1" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Clean Code Updated",
    "author": "Robert C. Martin",
    "isbn": "9780132350884"
  }'

echo ""
echo ""
echo "5. DELETE BOOK ID 1"
curl -i -X DELETE "$BASE_URL/1"

echo ""
echo ""
echo "6. VIEW ALL BOOKS AFTER DELETE"
curl -i "$BASE_URL"

echo ""
echo ""
echo "7. INVALID INPUT TEST"
curl -i -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "",
    "author": "",
    "isbn": ""
  }'

echo ""
echo ""
echo "Testing completed."
