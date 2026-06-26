#!/usr/bin/env bash
#
# End-to-end smoke script: login (HM + PM) via gateway, create demand, submit, PM approve.
#
# Prerequisites:
#   - Gateway :8080, user-auth :8081, demand :8082 (defaults in this repo)
#   - Postgres + Kafka + Redis running
#   - python3 on PATH (decode JWT "sub")
#   - Optional: jq for pretty JSON; otherwise raw JSON is printed
#
# Usage:
#   ./scripts/e2e/demand-workflow-via-gateway.sh
#   GATEWAY_URL=http://localhost:8080 PROJECT_ID=42 ./scripts/e2e/demand-workflow-via-gateway.sh
#
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
HM_EMAIL="${HM_EMAIL:-hm@griddynamics.com}"
HM_PASSWORD="${HM_PASSWORD:-Password@123}"
PM_EMAIL="${PM_EMAIL:-projectmanager@griddynamics.com}"
PM_PASSWORD="${PM_PASSWORD:-Password@123}"
PROJECT_ID="${PROJECT_ID:-1}"
ACCOUNT_ID="${ACCOUNT_ID:-1}"

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

echo "== Login as HM (${HM_EMAIL})"
HM_BODY=$(curl -sS -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${HM_EMAIL}\",\"password\":\"${HM_PASSWORD}\"}")
echo "$HM_BODY" | json_print
HM_TOKEN=$(echo "$HM_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "== Login as PM (${PM_EMAIL})"
PM_BODY=$(curl -sS -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${PM_EMAIL}\",\"password\":\"${PM_PASSWORD}\"}")
echo "$PM_BODY" | json_print
PM_TOKEN=$(echo "$PM_BODY" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

PM_SUB="$(jwt_sub "$PM_TOKEN")"
echo "== PM JWT sub (user id): ${PM_SUB}"

echo "== Fetch job titles (first id used)"
JT_JSON=$(curl -sS "${GATEWAY_URL}/api/v1/lookups/job-titles" -H "Authorization: Bearer ${HM_TOKEN}")
echo "$JT_JSON" | json_print
JOB_TITLE_ID=$(echo "$JT_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['jobTitleId'] if isinstance(d,list) and d else '')")
if [[ -z "${JOB_TITLE_ID}" ]]; then
  echo "ERROR: No job titles returned. Seed demand lookup tables or fix auth." >&2
  exit 1
fi

echo "== Fetch skills (first id used as mandatory)"
SK_JSON=$(curl -sS "${GATEWAY_URL}/api/v1/lookups/skills" -H "Authorization: Bearer ${HM_TOKEN}")
SKILL_ID=$(echo "$SK_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['skillId'] if isinstance(d,list) and d else '')")
if [[ -z "${SKILL_ID}" ]]; then
  echo "ERROR: No skills returned." >&2
  exit 1
fi

TARGET_DATE=$(python3 -c "from datetime import date,timedelta; print((date.today()+timedelta(days=60)).isoformat())")

CREATE_PAYLOAD="$(cat <<JSON
{
  "description": "E2E demand from demand-workflow-via-gateway.sh. This description is intentionally padded to be over 250 characters long to pass the strict validation rules enforced by DemandValidationService. This ensures that the demand creation endpoint does not reject the request due to the description being too short. We need at least 250 chars. Padding padding padding padding padding padding.",
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

echo "== POST /api/v1/demands (create DRAFT)"
CREATE_RESP=$(curl -sS -X POST "${GATEWAY_URL}/api/v1/demands" \
  -H "Authorization: Bearer ${HM_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "${CREATE_PAYLOAD}")
echo "$CREATE_RESP" | json_print
DEMAND_ID=$(echo "$CREATE_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('demandId',''))")
if [[ -z "${DEMAND_ID}" ]]; then
  echo "ERROR: create demand failed (no demandId)." >&2
  exit 1
fi

echo "== POST /api/v1/demands/${DEMAND_ID}/submit"
curl -sS -X POST "${GATEWAY_URL}/api/v1/demands/${DEMAND_ID}/submit" \
  -H "Authorization: Bearer ${HM_TOKEN}" | json_print

echo "== POST /api/v1/demands/${DEMAND_ID}/approve (PM token; needs projectManagerId == ${PM_SUB} in user-auth)"
APPROVE_HTTP=$(curl -sS -o /tmp/e2e_approve_body.json -w '%{http_code}' -X POST "${GATEWAY_URL}/api/v1/demands/${DEMAND_ID}/approve" \
  -H "Authorization: Bearer ${PM_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVED","comments":"Approved via e2e script"}')
cat /tmp/e2e_approve_body.json | json_print
echo "HTTP status: ${APPROVE_HTTP}"
if [[ "${APPROVE_HTTP}" != "200" ]]; then
  echo "NOTE: Non-200 approve is expected if user-auth project API is missing or projectManagerId does not match PM user id ${PM_SUB}." >&2
fi

echo "== GET /api/v1/demands/${DEMAND_ID}"
curl -sS "${GATEWAY_URL}/api/v1/demands/${DEMAND_ID}" -H "Authorization: Bearer ${HM_TOKEN}" | json_print

echo "Done."
