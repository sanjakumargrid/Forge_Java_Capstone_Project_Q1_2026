#!/bin/bash
# ============================================================================
# DEMAND ANALYTICS V1 API - QUICK REFERENCE & CURL EXAMPLES
# ============================================================================
#
# Endpoint: GET /api/v1/demands/analytics
# Authentication: JWT Bearer token (ANALYTICS_DEMAND_VIEW authority)
# Response Format: JSON (application/json)
# Performance Target: p95 < 500ms
#
# ============================================================================

# ─────────────────────────────────────────────────────────────────────────
# SETUP (Set these environment variables)
# ─────────────────────────────────────────────────────────────────────────

export API_BASE_URL="http://localhost:8080"  # or your service URL
export JWT_TOKEN="your-jwt-token-here"        # from auth service
export DEMAND_SERVICE_PORT=8080

# ─────────────────────────────────────────────────────────────────────────
# EXAMPLE 1: Basic Request (Last 30 days, all business units)
# ─────────────────────────────────────────────────────────────────────────

echo "📊 Example 1: Basic analytics (default 30-day window)"
curl -X GET \
  "${API_BASE_URL}/api/v1/demands/analytics" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n"

echo ""

# ─────────────────────────────────────────────────────────────────────────
# EXAMPLE 2: With Date Range
# ─────────────────────────────────────────────────────────────────────────

echo "📊 Example 2: Analytics for Q2 2026 (Jan-Jun)"
curl -X GET \
  "${API_BASE_URL}/api/v1/demands/analytics?dateFrom=2026-01-01&dateTo=2026-06-30" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n"

echo ""

# ─────────────────────────────────────────────────────────────────────────
# EXAMPLE 3: Filter by Business Unit
# ─────────────────────────────────────────────────────────────────────────

echo "📊 Example 3: Analytics for Engineering business unit, last 30 days"
curl -X GET \
  "${API_BASE_URL}/api/v1/demands/analytics?businessUnit=Engineering" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n"

echo ""

# ─────────────────────────────────────────────────────────────────────────
# EXAMPLE 4: All Filters Combined
# ─────────────────────────────────────────────────────────────────────────

echo "📊 Example 4: Analytics with date range + business unit filter"
curl -X GET \
  "${API_BASE_URL}/api/v1/demands/analytics?dateFrom=2026-01-01&dateTo=2026-06-21&businessUnit=Finance" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n"

echo ""

# ─────────────────────────────────────────────────────────────────────────
# EXAMPLE 5: Pretty-Print JSON Response (using jq)
# ─────────────────────────────────────────────────────────────────────────

echo "📊 Example 5: Pretty-printed response with jq"
curl -s -X GET \
  "${API_BASE_URL}/api/v1/demands/analytics?dateFrom=2026-05-01&dateTo=2026-06-21" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" | jq '.'

echo ""

# ─────────────────────────────────────────────────────────────────────────
# EXAMPLE 6: Extract Specific Metrics (using jq)
# ─────────────────────────────────────────────────────────────────────────

echo "📊 Example 6: Extract fill rate percentage only"
curl -s -X GET \
  "${API_BASE_URL}/api/v1/demands/analytics" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" | jq '.fillRate.percentageFilled'

echo ""

echo "📊 Example 7: Extract internal vs external split"
curl -s -X GET \
  "${API_BASE_URL}/api/v1/demands/analytics" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" | jq '.internalVsExternalSplit'

echo ""

echo "📊 Example 8: Extract capacity by project-client (top 5)"
curl -s -X GET \
  "${API_BASE_URL}/api/v1/demands/analytics" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" | jq '.capacityByProjectClient | sort_by(.demandCount) | reverse | .[0:5]'

echo ""

# ─────────────────────────────────────────────────────────────────────────
# EXAMPLE 9: Error Case - Missing JWT
# ─────────────────────────────────────────────────────────────────────────

echo "📊 Example 9: Request without JWT (expect 401 Unauthorized)"
curl -v -X GET \
  "${API_BASE_URL}/api/v1/demands/analytics" \
  -H "Content-Type: application/json" 2>&1 | grep -E "HTTP|WWW-Authenticate|Unauthorized"

echo ""

# ─────────────────────────────────────────────────────────────────────────
# EXAMPLE 10: Error Case - Invalid Date Format
# ─────────────────────────────────────────────────────────────────────────

echo "📊 Example 10: Request with invalid date format (expect 400 Bad Request)"
curl -X GET \
  "${API_BASE_URL}/api/v1/demands/analytics?dateFrom=05/22/2026" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n"

echo ""

# ─────────────────────────────────────────────────────────────────────────
# PERFORMANCE TESTING
# ─────────────────────────────────────────────────────────────────────────

echo "⚡ Performance Test: 10 concurrent requests"
for i in {1..10}; do
  curl -s -X GET \
    "${API_BASE_URL}/api/v1/demands/analytics" \
    -H "Authorization: Bearer ${JWT_TOKEN}" \
    -H "Content-Type: application/json" \
    -w "Request ${i}: %{time_total}s\n" \
    > /dev/null &
done
wait

echo "✅ Performance test complete"

# ─────────────────────────────────────────────────────────────────────────
# RESPONSE STRUCTURE REFERENCE
# ─────────────────────────────────────────────────────────────────────────

cat << 'EOF'

Response Structure:
{
  "metadata": {
    "dateFrom": "2026-05-22",           // Filter start date
    "dateTo": "2026-06-21",            // Filter end date
    "businessUnit": null,              // Applied BU filter (null if not filtered)
    "windowDays": 31                   // Days in analysis window
  },
  "fillRate": {
    "totalDemands": 250,               // Non-cancelled demands
    "filledDemands": 185,              // Demands with FILLED status
    "percentageFilled": 74.0           // (filled/total) * 100
  },
  "avgTimeToFillDays": 12.5,           // Average days to first FILLED status
  "internalVsExternalSplit": {
    "filledInternal": 110,             // closure_reason = 'FILLED_INTERNAL'
    "filledExternal": 75,              // closure_reason = 'FILLED_EXTERNAL'
    "percentageInternal": 59.46,       // (internal / total) * 100
    "percentageExternal": 40.54        // (external / total) * 100
  },
  "capacityByProjectClient": [         // Grouped by projectId + clientId
    {
      "projectId": 1001,
      "clientId": 5001,
      "demandCount": 15
    },
    ...
  ]
}

Query Parameters:
  ✓ dateFrom (optional): yyyy-MM-dd format; defaults to 30 days before dateTo
  ✓ dateTo (optional): yyyy-MM-dd format; defaults to today
  ✓ businessUnit (optional): exact match, case-sensitive; no filtering if omitted

HTTP Status Codes:
  200: Success
  400: Bad Request (invalid date format or parameters)
  401: Unauthorized (missing or invalid JWT)
  403: Forbidden (insufficient permissions)
  500: Server Error (database error, timeout, etc.)

Note: All timestamp calculations use server timezone. Ensure client converts tolocal time if needed.

RFC 3339 Example Usage:
  # Get analytics in ISO format for webhook/automation
  curl -s -X GET '...' | jq '.metadata | @json'

EOF

# ─────────────────────────────────────────────────────────────────────────
# TROUBLESHOOTING
# ─────────────────────────────────────────────────────────────────────────

cat << 'EOF'

Troubleshooting:

1. 401 Unauthorized:
   - Verify JWT token is valid and not expired
   - Check Authorization header format: "Bearer <token>"
   - Ensure token includes ANALYTICS_DEMAND_VIEW authority

2. 403 Forbidden:
   - User's JWT is valid but lacks ANALYTICS_DEMAND_VIEW permission
   - Contact security team to grant permission

3. 400 Bad Request:
   - Date format must be yyyy-MM-dd (e.g., 2026-06-21)
   - businessUnit must exactly match database values (case-sensitive)
   - URL-encode parameters if using query string with special chars

4. Slow Response (> 500ms):
   - Check database indexes (see INDEX_DDL_ANALYTICS_V1.sql)
   - Monitor demand table size and query performance
   - Consider caching responses if data doesn't change frequently

5. Missing Metrics:
   - Verify demand data exists in database for specified date range
   - Check that demands have status_history records (needed for time-to-fill)
   - Ensure closure_reason is set for filled demands

EOF

echo ""
echo "💡 For more details, see IMPLEMENTATION_SUMMARY.md and ANALYTICS_V1_SAMPLE_OUTPUT.json"
echo "✅ API documentation available at: ${API_BASE_URL}/swagger-ui.html"

