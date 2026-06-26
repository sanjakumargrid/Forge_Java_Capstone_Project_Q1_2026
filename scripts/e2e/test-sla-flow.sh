#!/bin/bash
set -e

# Login as HM
HM_LOGIN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dmalathy@griddynamics.com","password":"Password@123"}')
HM_TOKEN=$(echo $HM_LOGIN | python3 -c "import sys, json; print(json.load(sys.stdin).get('accessToken', ''))")

# Login as PM
PM_LOGIN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ppreetha@griddynamics.com","password":"Password@123"}')
PM_TOKEN=$(echo $PM_LOGIN | python3 -c "import sys, json; print(json.load(sys.stdin).get('accessToken', ''))")

echo "== POST /api/v1/demands (create DRAFT)"
TARGET_DATE=$(python3 -c "from datetime import date,timedelta; print((date.today()+timedelta(days=60)).isoformat())")

DEMAND_JSON=$(cat <<EOF
{
  "description": "E2E SLA test demand. This description is intentionally padded to be over 250 characters long to pass the strict validation rules enforced by DemandValidationService. This ensures that the demand creation endpoint does not reject the request due to the description being too short. We need at least 250 chars. Padding padding padding padding padding padding.",
  "level": "T4_STAFF",
  "location": "Remote",
  "accountId": 1,
  "projectId": 1,
  "businessUnit": "Engineering",
  "department": "Software Engineering",
  "jobTitleId": 8,
  "mandatorySkillIds": [ 14 ],
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
EOF
)

CREATE_RESP=$(curl -s -X POST http://localhost:8080/api/v1/demands \
  -H "Authorization: Bearer $HM_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$DEMAND_JSON")

DEMAND_ID=$(echo $CREATE_RESP | grep -o '"demandId":[0-9]*' | cut -d':' -f2)
if [ -z "$DEMAND_ID" ]; then
  echo "Failed to create demand:"
  echo $CREATE_RESP
  exit 1
fi
echo "Created demand ID: $DEMAND_ID"

echo "== POST /api/v1/demands/$DEMAND_ID/submit (Transition to PENDING_APPROVAL)"
curl -s -X POST http://localhost:8080/api/v1/demands/$DEMAND_ID/submit \
  -H "Authorization: Bearer $HM_TOKEN" > /dev/null

echo "== Waiting 2 seconds for DB write..."
sleep 2

echo "== Rewinding time by 25 hours to trigger 24h SLA reminder"
docker exec talentgrid-postgres psql -U talentgrid -d talentgrid -c "UPDATE demand_status_history SET changed_at = NOW() - INTERVAL '25 hours' WHERE demand_id = $DEMAND_ID AND to_status = 'PENDING_APPROVAL';" > /dev/null

echo "== Waiting 15 seconds for scheduler to run (runs every 10s)..."
sleep 15

echo "== Rewinding time by 75 hours to trigger 72h auto-cancellation"
docker exec talentgrid-postgres psql -U talentgrid -d talentgrid -c "UPDATE demand_status_history SET changed_at = NOW() - INTERVAL '75 hours' WHERE demand_id = $DEMAND_ID AND to_status = 'PENDING_APPROVAL';" > /dev/null

echo "== Waiting 15 seconds for scheduler to run..."
sleep 15

echo "== GET /api/v1/demands/$DEMAND_ID (Check Status)"
FINAL_RESP=$(curl -s -X GET http://localhost:8080/api/v1/demands/$DEMAND_ID \
  -H "Authorization: Bearer $HM_TOKEN")

echo $FINAL_RESP | jq . || echo $FINAL_RESP

echo "Done."
