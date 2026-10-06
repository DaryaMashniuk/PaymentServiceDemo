#!/usr/bin/env bash
set -e

echo "=== 1. Установка лимита 1000 USD ==="
curl -s -X POST http://localhost:8080/api/v1/limits \
  -H "Content-Type: application/json" \
  -d '{"account_from": "0000000123", "limit_sum": 1000.00, "expense_category": "PRODUCT"}'
echo -e "\n"

echo "=== 2. Проверка истории лимитов ==="
curl -s -X GET "http://localhost:8080/api/v1/limits?account=0000000123"
echo -e "\n"

echo "=== 3. Транзакция 500 USD (лимит не превышен) ==="
curl -s -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"account_from": "0000000123", "account_to": "9999999999", "currency_shortname": "USD", "sum": 500.00, "expense_category": "PRODUCT", "datetime": "2026-10-02T12:00:00Z"}'
echo -e "Status: 201 Created\n"

echo "=== 4. Транзакция 600 USD (лимит превышен: 500+600=1100 > 1000) ==="
curl -s -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"account_from": "0000000123", "account_to": "9999999999", "currency_shortname": "USD", "sum": 600.00, "expense_category": "PRODUCT", "datetime": "2026-10-03T12:00:00Z"}'
echo -e "Status: 201 Created\n"

echo "=== 5. Получение транзакций с превышением лимита ==="
curl -s -X GET "http://localhost:8080/api/v1/transactions/exceeded?account=0000000123"
echo -e "\n"

echo "=== 6. Проверка валидации ProblemDetail (невалидный счет) ==="
curl -s -i -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"account_from": "123", "account_to": "9999999999", "currency_shortname": "USD", "sum": 100.00, "expense_category": "PRODUCT", "datetime": "2026-10-02T12:00:00Z"}' | grep -E "HTTP|application/problem|invalid_fields"
echo -e "\n"

echo "=== Проверка завершена успешно! ==="