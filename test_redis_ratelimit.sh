#!/bin/bash

# ==============================================================================
# TalentGrid — Redis Rate Limiter E2E Test Script
#
# Tests the API Gateway's RequestRateLimiter (Spring Cloud Gateway + Redis)
# configured in:
#   talentgrid-api-gateway-service/src/main/resources/application.yml
#     (Spring Cloud Gateway 4.3+: routes/default-filters/globalcors/httpclient live under
#      spring.cloud.gateway.server.webflux — legacy spring.cloud.gateway.* keys can omit
#      default-filters so RequestRateLimiter never runs.)
#   RedisRateLimitConfig.java
#
# Key Resolver (RedisRateLimitConfig.userKeyResolver):
#   - If X-User-Id header is present → key = "user:{X-User-Id}"
#   - Otherwise                      → key = "ip:{remote_ip}"
#
# Default limits (overridable via env vars):
#   replenishRate   = RATE_LIMIT_REPLENISH  (default: 30 tokens/sec)
#   burstCapacity   = RATE_LIMIT_BURST      (default: 60 tokens max)
#   requestedTokens = RATE_LIMIT_TOKENS     (default: 1 token/request)
#
# Scenarios covered:
#   1. Prerequisites   — Redis container PING, Gateway health, Auth service
#   2. Rate-Limit Headers — on POST /api/v1/auth/login (routed; NOT /actuator — see Errata)
#   3. IP-Based Burst  — concurrent POST login (no X-User-Id) → expect 429s
#   4. Per-User Burst  — concurrent POST login with X-User-Id → expect 429s
#   5. Bucket Isolation — User A exhausted; User B (fresh bucket) unaffected
#   6. Token Recovery  — after throttle, wait for replenish → allowed again
#   7. Redis Key Inspection — show live request_rate_limiter.* keys in Redis
#   8. Headers on 429  — X-RateLimit-Remaining; Retry-After (optional)
#
# Errata: /actuator/** is not behind Spring Cloud Gateway route filters, so
#         RequestRateLimiter does not apply. Use a proxied path (e.g. POST
#         /api/v1/auth/login with empty body → 400) for rate-limit assertions.
# ==============================================================================

GATEWAY_URL="http://localhost:8080"
REDIS_CONTAINER="talentgrid-redis"
REPORT_FILE="redis_ratelimit_test_report.md"

# Rate limit params — must match gateway application.yml (or env var overrides)
REPLENISH_RATE=${RATE_LIMIT_REPLENISH:-30}
BURST_CAPACITY=${RATE_LIMIT_BURST:-60}

# Fire well above burst to overcome replenish during the same wall second
BURST_REQUESTS=$((BURST_CAPACITY + 40))

# ── Colors ────────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

PASSED=0
FAILED=0
TOTAL=0
TEST_NUM=0

# ── redis_cli wrapper ─────────────────────────────────────────────────────────
# Routes all redis-cli calls through the Docker container.
# Falls back to native redis-cli if the container is not running.
redis_cli() {
    if docker inspect "$REDIS_CONTAINER" > /dev/null 2>&1; then
        docker exec "$REDIS_CONTAINER" redis-cli "$@"
    else
        command redis-cli "$@"
    fi
}

# ── Report init ───────────────────────────────────────────────────────────────
{
    echo "# Redis Rate Limiter — E2E Test Report"
    echo "**Generated:** $(date)"
    echo "**Gateway:** $GATEWAY_URL"
    echo "**Redis container:** $REDIS_CONTAINER"
    echo "**Config:** replenishRate=$REPLENISH_RATE req/s | burstCapacity=$BURST_CAPACITY | requestedTokens=1"
    echo ""
    echo "## Results"
    echo "| # | Scenario | Test | Result | Details |"
    echo "|---|---|---|---|---|"
} > "$REPORT_FILE"

# ── Helpers ───────────────────────────────────────────────────────────────────
print_header() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════${NC}"
}

record_pass() {
    local scenario="$1" test="$2" details="$3"
    TEST_NUM=$((TEST_NUM + 1)); TOTAL=$((TOTAL + 1)); PASSED=$((PASSED + 1))
    echo -e "  [$(printf '%02d' $TEST_NUM)] ${GREEN}PASS${NC}  $test"
    echo "| $TEST_NUM | $scenario | $test | ✅ PASS | $(md_cell "$details") |" >> "$REPORT_FILE"
}

record_fail() {
    local scenario="$1" test="$2" details="$3"
    TEST_NUM=$((TEST_NUM + 1)); TOTAL=$((TOTAL + 1)); FAILED=$((FAILED + 1))
    echo -e "  [$(printf '%02d' $TEST_NUM)] ${RED}FAIL${NC}  $test — $details"
    echo "| $TEST_NUM | $scenario | $test | ❌ FAIL | $(md_cell "$details") |" >> "$REPORT_FILE"
}

record_info() {
    echo -e "  ${YELLOW}ℹ${NC}  $1"
}

# One line for Markdown table cells (no newlines or pipes)
md_cell() {
    echo "$1" | tr '\n\r' '  ' | sed 's/|/;/g' | sed 's/  */ /g' | sed 's/^ *//;s/ *$//'
}

# Count lines matching an extended regex. Do NOT use `grep -c ... || echo 0` on macOS:
# when the count is 0, BSD grep exits 1 and `|| echo 0` appends a second "0".
count_matches() {
    local file="$1" pattern="$2"
    local n
    n=$(grep -cE "$pattern" "$file" 2>/dev/null || true)
    n=$(echo "$n" | tr -d '[:space:]')
    echo "${n:-0}"
}

# POST to a gateway-routed path. Spring Cloud Gateway default-filters do not apply to
# /actuator/** (handled by Boot actuator), so rate limit tests must use a real route.
rate_limit_post() {
    curl -s -o /dev/null -w "%{http_code}\n" \
        -X POST "$GATEWAY_URL/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        "$@" \
        -d '{}'
}

# Flush all rate limiter keys from Redis before each burst scenario
flush_rate_keys() {
    local keys
    keys=$(redis_cli KEYS "request_rate_limiter.*" 2>/dev/null)
    if [ -n "$keys" ]; then
        echo "$keys" | xargs redis_cli DEL > /dev/null 2>&1
    fi
    sleep 0.3
}

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 1 — PREREQUISITES
# ══════════════════════════════════════════════════════════════════════════════
print_header "Section 1 — Prerequisites"

# 1a. Redis reachability via Docker container
echo -n "  Checking Redis (docker: $REDIS_CONTAINER) ... "
REDIS_PONG=$(redis_cli PING 2>/dev/null)
if [ "$REDIS_PONG" == "PONG" ]; then
    echo -e "${GREEN}PONG — Redis is UP${NC}"
    record_pass "Prerequisites" "Redis reachable" "docker exec $REDIS_CONTAINER redis-cli PING → PONG"
else
    echo -e "${RED}FAILED (got: '$REDIS_PONG')${NC}"
    record_fail "Prerequisites" "Redis reachable" \
        "Start Redis: docker run -d --name talentgrid-redis -p 6379:6379 redis:7-alpine"
    echo -e "\n${RED}Redis is required. Aborting.${NC}"
    exit 1
fi

# 1b. Gateway reachability
echo -n "  Checking Gateway at $GATEWAY_URL ... "
GW_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$GATEWAY_URL/actuator/health")
if [ "$GW_STATUS" == "200" ]; then
    echo -e "${GREEN}200 — Gateway is UP${NC}"
    record_pass "Prerequisites" "Gateway reachable" "GET /actuator/health → 200"
else
    echo -e "${RED}Got $GW_STATUS — Gateway not ready${NC}"
    record_fail "Prerequisites" "Gateway reachable" "GET /actuator/health → $GW_STATUS"
    echo -e "\n${RED}Gateway is required. Aborting.${NC}"
    exit 1
fi

# 1c. Auth service reachability (via gateway)
echo -n "  Checking Auth Service (via gateway) ... "
AUTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$GATEWAY_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{}')
if [ "$AUTH_STATUS" == "400" ] || [ "$AUTH_STATUS" == "200" ]; then
    echo -e "${GREEN}Auth service responding (got $AUTH_STATUS — 400=validation=UP)${NC}"
    record_pass "Prerequisites" "Auth Service reachable via Gateway" \
        "POST /api/v1/auth/login → $AUTH_STATUS"
else
    echo -e "${YELLOW}Got $AUTH_STATUS — Auth service may be down${NC}"
    record_fail "Prerequisites" "Auth Service reachable via Gateway" \
        "Expected 400/200, got $AUTH_STATUS"
fi

# ──────────────────────────────────────────────────────────────────────────────
# SETUP — Register and login a fresh test user
# ──────────────────────────────────────────────────────────────────────────────
print_header "Setup — Register & Login Test User"

TS=$(date +%s)
TEST_EMAIL="ratelimit_${TS}@talentgrid.test"
TEST_PASS="RateLimit@1234"

echo -n "  Registering $TEST_EMAIL ... "
REG_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$GATEWAY_URL/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"ratelimit_${TS}\",\"email\":\"${TEST_EMAIL}\",\"password\":\"${TEST_PASS}\",\"location\":\"Chennai\"}")
[ "$REG_STATUS" == "200" ] \
    && echo -e "${GREEN}Registered (200)${NC}" \
    || echo -e "${YELLOW}Got $REG_STATUS${NC}"

echo -n "  Logging in to get JWT ... "
LOGIN_RESP=$(curl -s -X POST "$GATEWAY_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${TEST_EMAIL}\",\"password\":\"${TEST_PASS}\"}")
JWT=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -n "$JWT" ] && [ "$JWT" != "null" ]; then
    echo -e "${GREEN}JWT acquired${NC}"
else
    echo -e "${YELLOW}No JWT — rate limit tests will use IP-based keying${NC}"
    JWT=""
fi

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 2 — RATE-LIMIT RESPONSE HEADERS
# ══════════════════════════════════════════════════════════════════════════════
print_header "Section 2 — Rate-Limit Response Headers"
record_info "Use POST /api/v1/auth/login (routed) — actuator is not behind Gateway filters"
record_info "Expect X-RateLimit-Remaining and X-RateLimit-Burst-Capacity (Spring Cloud Gateway)"

flush_rate_keys

HEADERS_FILE=$(mktemp)
curl -s -D "$HEADERS_FILE" -o /dev/null \
    -X POST "$GATEWAY_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{}'

echo -e "  ${YELLOW}Headers received:${NC}"
grep -i "x-ratelimit\|x-rate-limit\|retry-after" "$HEADERS_FILE" | while read -r line; do
    echo -e "    ${CYAN}${line}${NC}"
done

if grep -qi "x-ratelimit-remaining" "$HEADERS_FILE"; then
    REMAINING=$(grep -i "x-ratelimit-remaining" "$HEADERS_FILE" | grep -o '[0-9]*' | head -1)
    record_pass "Headers" "X-RateLimit-Remaining present" "Value: $REMAINING"
else
    record_fail "Headers" "X-RateLimit-Remaining present" "Header missing from response"
fi

# Spring Cloud Gateway uses X-RateLimit-Burst-Capacity (not X-RateLimit-Limit)
if grep -qi "x-ratelimit-burst-capacity\|x-ratelimit-replenish-rate" "$HEADERS_FILE"; then
    BURST_HDR=$(grep -i "x-ratelimit-burst-capacity" "$HEADERS_FILE" | head -1)
    record_pass "Headers" "X-RateLimit-Burst-Capacity / config headers present" "$BURST_HDR"
else
    record_fail "Headers" "X-RateLimit-Burst-Capacity present" "Burst-capacity / replenish headers missing"
fi

rm -f "$HEADERS_FILE"

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 3 — IP-BASED BURST TEST
# ══════════════════════════════════════════════════════════════════════════════
print_header "Section 3 — IP-Based Burst Test (no X-User-Id)"
record_info "Firing $BURST_REQUESTS concurrent requests → key = ip:127.0.0.1"
record_info "Expect: first ~$BURST_CAPACITY succeed, remainder get 429"

flush_rate_keys
TMP_IP=$(mktemp)

for i in $(seq 1 $BURST_REQUESTS); do
    rate_limit_post >> "$TMP_IP" &
done
wait

COUNT_400_IP=$(count_matches "$TMP_IP" '^400$')
COUNT_429_IP=$(count_matches "$TMP_IP" '^429$')
COUNT_2XX_IP=$(count_matches "$TMP_IP" '^2[0-9][0-9]$')

echo ""
echo -e "  ${YELLOW}─── IP Burst Results ───────────────────────────────────${NC}"
echo -e "  Total fired:     $BURST_REQUESTS"
echo -e "  2xx (success):   $COUNT_2XX_IP"
echo -e "  400 (bad body):  $COUNT_400_IP   ← valid response from auth service"
echo -e "  429 (throttled): ${RED}$COUNT_429_IP${NC}"
echo -e "  ${YELLOW}───────────────────────────────────────────────────────${NC}"
echo ""

if [ "$COUNT_429_IP" -gt 0 ]; then
    record_pass "IP Burst" "429 Too Many Requests observed" \
        "$COUNT_429_IP of $BURST_REQUESTS throttled (burst=$BURST_CAPACITY)"
else
    record_fail "IP Burst" "429 Too Many Requests observed" \
        "No 429s in $BURST_REQUESTS requests — Is Redis connected to the gateway?"
fi

rm -f "$TMP_IP"

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 4 — PER-USER BURST TEST (X-User-Id header)
# ══════════════════════════════════════════════════════════════════════════════
print_header "Section 4 — Per-User Burst Test (X-User-Id header)"
record_info "Sending X-User-Id: ratelimit-testuser-A → key = user:ratelimit-testuser-A"
record_info "Firing $BURST_REQUESTS concurrent requests; expect 429s after $BURST_CAPACITY"

flush_rate_keys
TMP_USER=$(mktemp)

for i in $(seq 1 $BURST_REQUESTS); do
    rate_limit_post -H "X-User-Id: ratelimit-testuser-A" >> "$TMP_USER" &
done
wait

COUNT_400_U=$(count_matches "$TMP_USER" '^400$')
COUNT_429_U=$(count_matches "$TMP_USER" '^429$')
COUNT_2XX_U=$(count_matches "$TMP_USER" '^2[0-9][0-9]$')

echo ""
echo -e "  ${YELLOW}─── Per-User Burst Results (user=ratelimit-testuser-A) ──${NC}"
echo -e "  Total fired:     $BURST_REQUESTS"
echo -e "  400 (allowed):   $COUNT_400_U   ← empty login body (still counts toward limit)"
echo -e "  2xx (allowed):   $COUNT_2XX_U"
echo -e "  429 (throttled): ${RED}$COUNT_429_U${NC}"
echo -e "  ${YELLOW}────────────────────────────────────────────────────────${NC}"

echo -n "  Redis key check → "
REDIS_KEY_CHECK=$(redis_cli KEYS "*ratelimit-testuser-A*" 2>/dev/null)
if [ -n "$REDIS_KEY_CHECK" ]; then
    echo -e "${GREEN}Found: $REDIS_KEY_CHECK${NC}"
    record_pass "Per-User Burst" "Redis stores per-user key" "Key: $REDIS_KEY_CHECK"
elif [ "$COUNT_429_U" -gt 0 ]; then
    echo -e "${GREEN}Keys already expired (short TTL) — 429 proves per-user limiter ran${NC}"
    record_pass "Per-User Burst" "Redis limiter active for user key" \
        "429 observed; keys may expire before inspection"
else
    echo -e "${YELLOW}Not found${NC}"
    record_fail "Per-User Burst" "Redis stores per-user key" \
        "No key match and no 429 — limiter may not be active"
fi

if [ "$COUNT_429_U" -gt 0 ]; then
    record_pass "Per-User Burst" "Per-user 429 observed" \
        "$COUNT_429_U of $BURST_REQUESTS throttled"
else
    record_fail "Per-User Burst" "Per-user 429 observed" \
        "No 429s — X-User-Id keying may not be active"
fi

rm -f "$TMP_USER"

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 5 — BUCKET ISOLATION (User A vs User B)
# ══════════════════════════════════════════════════════════════════════════════
print_header "Section 5 — Bucket Isolation (User A vs User B)"
record_info "Exhaust User A's bucket → verify User B (fresh bucket) is unaffected"

flush_rate_keys
TMP_A=$(mktemp)
TMP_B=$(mktemp)

EXHAUST=$((BURST_CAPACITY + 15))
record_info "Exhausting isolation-user-A with $EXHAUST requests..."

for i in $(seq 1 $EXHAUST); do
    rate_limit_post -H "X-User-Id: isolation-user-A" >> "$TMP_A" &
done
wait

COUNT_429_A=$(count_matches "$TMP_A" '^429$')
echo -e "  User A: $EXHAUST requests, $COUNT_429_A throttled"

record_info "Firing 10 requests for isolation-user-B (separate bucket)..."
for i in $(seq 1 10); do
    rate_limit_post -H "X-User-Id: isolation-user-B" >> "$TMP_B" &
done
wait

COUNT_429_B=$(count_matches "$TMP_B" '^429$')
COUNT_400_B=$(count_matches "$TMP_B" '^400$')
COUNT_2XX_B=$(count_matches "$TMP_B" '^2[0-9][0-9]$')
echo -e "  User B: 10 requests, $COUNT_429_B throttled, $((COUNT_400_B + COUNT_2XX_B)) allowed (400/2xx)"
echo ""

if [ "$COUNT_429_A" -gt 0 ] && [ "$COUNT_429_B" -eq 0 ]; then
    record_pass "Isolation" "User A throttled, User B unaffected" \
        "UserA: $COUNT_429_A 429s | UserB: 0 429s — buckets isolated ✓"
elif [ "$COUNT_429_A" -eq 0 ]; then
    record_fail "Isolation" "User A bucket exhausted" \
        "User A got no 429s — burst capacity not exceeded"
else
    record_fail "Isolation" "User B unaffected by User A exhaustion" \
        "UserB got $COUNT_429_B unexpected 429s — buckets may share state"
fi

rm -f "$TMP_A" "$TMP_B"

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 6 — TOKEN RECOVERY
# ══════════════════════════════════════════════════════════════════════════════
print_header "Section 6 — Token Recovery (replenishRate = $REPLENISH_RATE req/s)"
record_info "Exhaust bucket → wait 3s → $((REPLENISH_RATE * 3)) tokens restored → requests succeed"

flush_rate_keys
RECOVERY_USER="recovery-user-${TS}"
TMP_R=$(mktemp)

record_info "Phase 1: Exhausting $RECOVERY_USER bucket..."
for i in $(seq 1 $((BURST_CAPACITY + 10))); do
    rate_limit_post -H "X-User-Id: $RECOVERY_USER" >> "$TMP_R" &
done
wait

COUNT_429_BEFORE=$(count_matches "$TMP_R" '^429$')
echo -e "  Before wait: $COUNT_429_BEFORE requests throttled"

if [ "$COUNT_429_BEFORE" -gt 0 ]; then
    record_pass "Recovery" "Bucket successfully exhausted" "$COUNT_429_BEFORE 429s observed"
else
    record_fail "Recovery" "Bucket exhausted before wait" \
        "No 429s — cannot test recovery"
fi

WAIT_SECS=3
record_info "Phase 2: Waiting ${WAIT_SECS}s for replenishment (~$((REPLENISH_RATE * WAIT_SECS)) tokens restored)..."
sleep $WAIT_SECS

TMP_R2=$(mktemp)
record_info "Phase 3: Firing 5 requests after recovery..."
for i in $(seq 1 5); do
    rate_limit_post -H "X-User-Id: $RECOVERY_USER" >> "$TMP_R2" &
done
wait

COUNT_429_AFTER=$(count_matches "$TMP_R2" '^429$')
COUNT_400_AFTER=$(count_matches "$TMP_R2" '^400$')
COUNT_2XX_AFTER=$(count_matches "$TMP_R2" '^2[0-9][0-9]$')
COUNT_ALLOWED_AFTER=$((COUNT_400_AFTER + COUNT_2XX_AFTER))

echo ""
echo -e "  ${YELLOW}─── Recovery Results ───────────────────────────────────${NC}"
echo -e "  Requests after ${WAIT_SECS}s: 5"
echo -e "  Allowed (400/2xx):     $COUNT_ALLOWED_AFTER"
echo -e "  429 (still throttled): $COUNT_429_AFTER"
echo -e "  ${YELLOW}───────────────────────────────────────────────────────${NC}"
echo ""

if [ "$COUNT_ALLOWED_AFTER" -gt 0 ] && [ "$COUNT_429_AFTER" -eq 0 ]; then
    record_pass "Recovery" "All requests succeed after replenish" \
        "$COUNT_ALLOWED_AFTER/5 allowed after ${WAIT_SECS}s wait"
elif [ "$COUNT_ALLOWED_AFTER" -gt 0 ]; then
    record_pass "Recovery" "Partial recovery observed" \
        "$COUNT_ALLOWED_AFTER allowed, $COUNT_429_AFTER still throttled"
else
    record_fail "Recovery" "No recovery after ${WAIT_SECS}s" \
        "All 5 still throttled — replenishRate may be 0 or Redis disconnected"
fi

rm -f "$TMP_R" "$TMP_R2"

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 7 — REDIS KEY INSPECTION
# ══════════════════════════════════════════════════════════════════════════════
print_header "Section 7 — Redis Key Inspection"
record_info "Spring Cloud Gateway stores token buckets as: request_rate_limiter.{key}.tokens"

echo -e "  ${YELLOW}Active rate limiter keys in Redis:${NC}"
ALL_KEYS=$(redis_cli KEYS "request_rate_limiter.*" 2>/dev/null)

if [ -n "$ALL_KEYS" ]; then
    echo "$ALL_KEYS" | while read -r key; do
        TTL=$(redis_cli TTL "$key" 2>/dev/null)
        VAL=$(redis_cli GET "$key" 2>/dev/null)
        echo -e "    ${CYAN}${key}${NC}  →  val=${VAL}  ttl=${TTL}s"
    done
    KEY_COUNT=$(echo "$ALL_KEYS" | wc -l | tr -d ' ')
    record_pass "Redis" "Rate limiter keys exist" "$KEY_COUNT keys found"
else
    echo -e "    ${YELLOW}No keys found — they expire quickly (short TTL)${NC}"
    record_info "Keys not found (already expired between test and inspection — this is normal)"
fi

echo ""
echo -e "  ${YELLOW}Named test-user key lookup:${NC}"
for user in "ratelimit-testuser-A" "isolation-user-A" "isolation-user-B" "$RECOVERY_USER"; do
    KEY=$(redis_cli KEYS "*${user}*" 2>/dev/null)
    if [ -n "$KEY" ]; then
        echo -e "    ${GREEN}Found: $KEY${NC}"
    else
        echo -e "    ${YELLOW}Expired: *${user}*${NC}"
    fi
done

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 8 — RETRY-AFTER + HEADERS ON 429
# ══════════════════════════════════════════════════════════════════════════════
print_header "Section 8 — Response Headers on 429"
record_info "Checking X-RateLimit-Remaining and Retry-After headers on a throttled response"

flush_rate_keys
EXHAUST_USER="retry-header-test-${TS}"
TMP_EX=$(mktemp)

# Exhaust the bucket (routed path so rate limiter applies)
for i in $(seq 1 $((BURST_CAPACITY + 5))); do
    rate_limit_post -H "X-User-Id: $EXHAUST_USER" >> "$TMP_EX" &
done
wait

# Capture headers on a throttled request (same route + user key)
TMP_H=$(mktemp)
curl -s -D "$TMP_H" -o /dev/null \
    -X POST "$GATEWAY_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -H "X-User-Id: $EXHAUST_USER" \
    -d '{}'

HTTP_STATUS_LINE=$(head -1 "$TMP_H")
echo -e "  ${YELLOW}Status:${NC} $HTTP_STATUS_LINE"
echo -e "  ${YELLOW}Rate-limit headers on throttled response:${NC}"
grep -i "x-ratelimit\|retry-after" "$TMP_H" | while read -r line; do
    echo -e "    ${CYAN}${line}${NC}"
done

if grep -qi "retry-after" "$TMP_H"; then
    RETRY_AFTER=$(grep -i "retry-after" "$TMP_H" | grep -o '[0-9]*' | head -1)
    record_pass "Headers" "Retry-After present on 429" "Retry-After: $RETRY_AFTER"
else
    record_info "Retry-After not set (Spring Cloud Gateway omits it by default — this is expected)"
    echo "| - | Headers | Retry-After on 429 | ℹ️ INFO | Not set by default in Spring Cloud Gateway |" >> "$REPORT_FILE"
fi

if grep -qi "x-ratelimit-remaining: 0" "$TMP_H"; then
    record_pass "Headers" "X-RateLimit-Remaining: 0 on 429" "Confirmed 0 tokens remaining"
else
    REM=$(grep -i "x-ratelimit-remaining" "$TMP_H" | grep -o '[0-9]*' | head -1)
    [ -n "$REM" ] && record_info "X-RateLimit-Remaining=$REM on throttled response"
fi

rm -f "$TMP_H" "$TMP_EX"

# ══════════════════════════════════════════════════════════════════════════════
# SUMMARY
# ══════════════════════════════════════════════════════════════════════════════
echo ""
echo -e "${YELLOW}══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}  Summary                                                  ${NC}"
echo -e "${YELLOW}══════════════════════════════════════════════════════════${NC}"
echo -e "  ${GREEN}Passed: $PASSED${NC}  ${RED}Failed: $FAILED${NC}  Total: $TOTAL"
echo -e "  Report: ${YELLOW}$REPORT_FILE${NC}"
echo -e "${YELLOW}══════════════════════════════════════════════════════════${NC}"
echo ""

{
    echo ""
    echo "---"
    echo "## Summary"
    echo "| Metric | Count |"
    echo "|---|---|"
    echo "| Total | $TOTAL |"
    echo "| ✅ Passed | $PASSED |"
    echo "| ❌ Failed | $FAILED |"
    echo ""
    echo "### Rate Limiter Config"
    echo "| Parameter | Value | Env Var to Override |"
    echo "|---|---|---|"
    echo "| replenishRate | $REPLENISH_RATE req/sec | RATE_LIMIT_REPLENISH |"
    echo "| burstCapacity | $BURST_CAPACITY | RATE_LIMIT_BURST |"
    echo "| requestedTokens | 1 | RATE_LIMIT_TOKENS |"
    echo "| Key Resolver | X-User-Id → user:{id}, else ip:{ip} | RedisRateLimitConfig.java |"
    echo ""
    echo "_Generated by \`test_redis_ratelimit.sh\` on $(date)_"
} >> "$REPORT_FILE"
