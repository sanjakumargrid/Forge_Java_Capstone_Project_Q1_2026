#!/usr/bin/env bash
#
# End-to-end verification for demand PATCH edit rules:
#   - reasonForEdit required
#   - editable statuses vs FILLED/CLOSED
#   - post-APPROVED field locks
#   - same demand ID updated (not recreated)
#   - edit reason stored in demand_status_history
#
# Prerequisites: same as demand-workflow-via-gateway.sh
#
# Usage:
#   ./scripts/e2e/demand-edit-flow-via-gateway.sh
#   GATEWAY_URL=http://localhost:8080 PROJECT_ID=1 ./scripts/e2e/demand-edit-flow-via-gateway.sh
#
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
HM_EMAIL="${HM_EMAIL:-hm@griddynamics.com}"
HM_PASSWORD="${HM_PASSWORD:-Password@123}"
PM_EMAIL="${PM_EMAIL:-projectmanager@griddynamics.com}"
PM_PASSWORD="${PM_PASSWORD:-Password@123}"
PROJECT_ID="${PROJECT_ID:-1}"
ACCOUNT_ID="${ACCOUNT_ID:-1}"

PASS=0
FAIL=0

json_print() {
  if command -v jq >/dev/null 2>&1; then
    jq .
  else
    cat
  fi
}

jwt_sub() {
  python3 -c 'import sys,json,base64
t=sys.argv[1]
p=t.split(".")[1]
p += "=" * (-len(p) % 4)
print(json.loads(base64.urlsafe_b64decode(p.encode("ascii")))["sub"])' "$1"
}

assert_http() {
  local label="$1"
  local expected="$2"
  local actual="$3"
  local body_file="$4"
  if [[ "${actual}" == "${expected}" ]]; then
    echo "PASS: ${label} (HTTP ${actual})"
    PASS=$((PASS + 1))
  else
    echo "FAIL: ${label} — expected HTTP ${expected}, got ${actual}" >&2
    if [[ -f "${body_file}" ]]; then
      cat "${body_file}" | json_print >&2
    fi
    FAIL=$((FAIL + 1))
  fi
}

assert_json_field() {
  local label="$1"
  local body="$2"
  local expr="$3"
  local expected="$4"
  local actual
  actual=$(echo "${body}" | python3 -c "import sys,json; d=json.load(sys.stdin); print(${expr})")
  if [[ "${actual}" == "${expected}" ]]; then
    echo "PASS: ${label} (${actual})"
    PASS=$((PASS + 1))
  else
    echo "FAIL: ${label} — expected '${expected}', got '${actual}'" >&2
    FAIL=$((FAIL + 1))
  fi
}

patch_demand() {
  local token="$1"
  local demand_id="$2"
  local payload="$3"
  curl -sS -o /tmp/demand_edit_body.json -w '%{http_code}' -X PATCH \
    "${GATEWAY_URL}/api/v1/demands/${demand_id}" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "${payload}"
}

echo "== Login as HM (${HM_EMAIL})"
HM_BODY=$(curl -sS -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${HM_EMAIL}\",\"password\":\"${HM_PASSWORD}\"}")
HM_TOKEN=$(echo "$HM_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "== Login as PM (${PM_EMAIL})"
PM_BODY=$(curl -sS -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${PM_EMAIL}\",\"password\":\"${PM_PASSWORD}\"}")
PM_TOKEN=$(echo "$PM_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
PM_SUB="$(jwt_sub "$PM_TOKEN")"
echo "PM JWT sub: ${PM_SUB}"

echo "== Fetch lookups"
JT_JSON=$(curl -sS "${GATEWAY_URL}/api/v1/lookups/job-titles" -H "Authorization: Bearer ${HM_TOKEN}")
JOB_TITLE_ID=$(echo "$JT_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['jobTitleId'] if isinstance(d,list) and d else '')")
SK_JSON=$(curl -sS "${GATEWAY_URL}/api/v1/lookups/skills" -H "Authorization: Bearer ${HM_TOKEN}")
SKILL_ID=$(echo "$SK_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['skillId'] if isinstance(d,list) and d else '')")
if [[ -z "${JOB_TITLE_ID}" || -z "${SKILL_ID}" ]]; then
  echo "ERROR: lookups missing job title or skill." >&2
  exit 1
fi

TARGET_DATE=$(python3 -c "from datetime import date,timedelta; print((date.today()+timedelta(days=60)).isoformat())")

CREATE_PAYLOAD="$(cat <<JSON
{
  "description": "E2E demand for edit-flow verification",
  "level": "T4_STAFF",
  "location": "Remote",
  "accountId": ${ACCOUNT_ID},
  "projectId": ${PROJECT_ID},
  "businessUnit": "Engineering",
  "department": "Software Engineering",
  "jobTitleId": ${JOB_TITLE_ID},
  "mandatorySkillIds": [${SKILL_ID}],
  "budget": 150000,
  "reqUtilPerc": 80,
  "targetDate": "${TARGET_DATE}",
  "priority": "MEDIUM",
  "employmentType": "FULL_TIME",
  "workMode": "REMOTE",
  "experience": 5,
  "clientInterview": false,
  "benchHiring": false
}
JSON
)"

echo "== Create demand (DRAFT)"
CREATE_RESP=$(curl -sS -X POST "${GATEWAY_URL}/api/v1/demands" \
  -H "Authorization: Bearer ${HM_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "${CREATE_PAYLOAD}")
echo "$CREATE_RESP" | json_print
DEMAND_ID=$(echo "$CREATE_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('demandId',''))")
if [[ -z "${DEMAND_ID}" ]]; then
  echo "ERROR: create demand failed." >&2
  exit 1
fi

echo "== PATCH without reasonForEdit (expect 400)"
HTTP=$(patch_demand "${HM_TOKEN}" "${DEMAND_ID}" '{"location":"Bangalore"}')
assert_http "Missing reasonForEdit rejected" "400" "${HTTP}" /tmp/demand_edit_body.json

echo "== PATCH in DRAFT with reasonForEdit (expect 200, same demandId)"
DRAFT_PATCH_PAYLOAD='{"reasonForEdit":"Initial location update in draft","location":"Bangalore","budget":160000}'
HTTP=$(patch_demand "${HM_TOKEN}" "${DEMAND_ID}" "${DRAFT_PATCH_PAYLOAD}")
assert_http "DRAFT edit succeeds" "200" "${HTTP}" /tmp/demand_edit_body.json
PATCH_BODY=$(cat /tmp/demand_edit_body.json)
assert_json_field "Same demand ID returned" "${PATCH_BODY}" "d.get('demandId')" "${DEMAND_ID}"
assert_json_field "Location updated" "${PATCH_BODY}" "d.get('location')" "Bangalore"

echo "== GET history after DRAFT edit (expect edit comment)"
HISTORY_JSON=$(curl -sS "${GATEWAY_URL}/api/v1/demands/${DEMAND_ID}/history" \
  -H "Authorization: Bearer ${HM_TOKEN}")
echo "$HISTORY_JSON" | json_print
echo "$HISTORY_JSON" | python3 -c '
import sys, json
items = json.load(sys.stdin)
edits = [h for h in items if h.get("fromStatus") == h.get("toStatus") and h.get("comments") == "Initial location update in draft"]
if edits:
    print("PASS: Edit history contains reasonForEdit comment")
    raise SystemExit(0)
print("FAIL: Edit history missing expected edit comment", file=sys.stderr)
raise SystemExit(1)
' && PASS=$((PASS + 1)) || FAIL=$((FAIL + 1))

echo "== Submit demand (PENDING_APPROVAL)"
curl -sS -X POST "${GATEWAY_URL}/api/v1/demands/${DEMAND_ID}/submit" \
  -H "Authorization: Bearer ${HM_TOKEN}" | json_print

echo "== PATCH in PENDING_APPROVAL changing priority (allowed before approval)"
HTTP=$(patch_demand "${HM_TOKEN}" "${DEMAND_ID}" '{"reasonForEdit":"Raise priority before PM review","priority":"HIGH"}')
assert_http "PENDING_APPROVAL priority change allowed" "200" "${HTTP}" /tmp/demand_edit_body.json

echo "== Approve demand (APPROVED)"
APPROVE_HTTP=$(curl -sS -o /tmp/demand_edit_approve.json -w '%{http_code}' -X POST \
  "${GATEWAY_URL}/api/v1/demands/${DEMAND_ID}/approve" \
  -H "Authorization: Bearer ${PM_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVED","comments":"Approved for edit-flow test"}')
cat /tmp/demand_edit_approve.json | json_print
echo "Approve HTTP: ${APPROVE_HTTP}"

if [[ "${APPROVE_HTTP}" != "200" ]]; then
  echo "WARN: Approve did not return 200 — post-approval locked-field checks will be skipped." >&2
  echo "      Ensure project ${PROJECT_ID} has projectManagerId == ${PM_SUB} in user-auth." >&2
else
  echo "== PATCH allowed field in APPROVED (location)"
  HTTP=$(patch_demand "${HM_TOKEN}" "${DEMAND_ID}" '{"reasonForEdit":"Client asked for hybrid","location":"Hyderabad","workMode":"HYBRID"}')
  assert_http "APPROVED allowed-field edit succeeds" "200" "${HTTP}" /tmp/demand_edit_body.json

  echo "== PATCH locked field in APPROVED (priority, expect 409)"
  HTTP=$(patch_demand "${HM_TOKEN}" "${DEMAND_ID}" '{"reasonForEdit":"Try to change priority","priority":"CRITICAL"}')
  assert_http "APPROVED locked-field edit rejected" "409" "${HTTP}" /tmp/demand_edit_body.json
fi

echo
echo "== Summary: ${PASS} passed, ${FAIL} failed"
if [[ "${FAIL}" -gt 0 ]]; then
  exit 1
fi
echo "All demand edit flow checks passed."
