# Workforce Service - Local Run and JWT Testing Guide

This guide explains the standard end-to-end way to run and test `workforce-service` with authentication from `user-auth-service`.

## 1) What this service expects

- Port: `8091`
- Database: PostgreSQL (`talentgrid`)
- Auth source: `user-auth-service` JWT token (HS256)
- Shared JWT secret: `JWT_SECRET` must be identical in both auth and workforce services
- Downstream dependency: `demand-service` for demand APIs

---

## 2) Pre-requisites

- Java 17+
- Maven 3.8+
- PostgreSQL running on `localhost:5432`
- Redis running on `localhost:6379` (needed by auth service)

---

## 3) Environment variables (recommended)

Set these before starting services:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/talentgrid"
export DB_USERNAME="postgres"
export DB_PASSWORD="abhi"
export JWT_SECRET="my-super-secure-jwt-secret-key-1234567890"
export DEMAND_SERVICE_URL="http://localhost:8081"
```

> `JWT_SECRET` must exactly match in both `user-auth-service` and `workforce-service`.

---

## 4) Build order (important)

From project root:

```bash
mvn -pl talentgrid-shared clean install -DskipTests
mvn -pl services/user-auth-service clean install -DskipTests
mvn -pl services/demand-service clean install -DskipTests
mvn -pl services/workforce-service clean install -DskipTests
```

---

## 5) Start applications (in separate terminals)

### 5.1 Start user-auth-service (port 8080)

```bash
cd services/user-auth-service
mvn spring-boot:run
```

### 5.2 Start demand-service (port 8081)

```bash
cd services/demand-service
mvn spring-boot:run
```

### 5.3 Start workforce-service (port 8091)

```bash
cd services/workforce-service
mvn spring-boot:run
```

---

## 6) SQL setup for workforce authorization scopes

Run on the auth database (`talentgrid`) to ensure required scopes exist and are mapped to role(s).

```sql
-- 1) Insert workforce scopes if missing
INSERT INTO scopes (name, description) VALUES
('WORKFORCE_NOMINATION_CREATE', 'Submit a manual nomination for an engineer'),
('WORKFORCE_NOMINATION_VIEW', 'View nominations submitted for demands/engineers'),
('WORKFORCE_BENCH_SEARCH', 'Search internal employees/engineers currently on the bench'),
('WORKFORCE_ANALYTICS_VIEW', 'Access the RMG analytics and dashboard statistics'),
('WORKFORCE_SKILLGAP_VIEW', 'View the skill gap heatmap'),
('WORKFORCE_SKILLGAP_REFRESH', 'Refresh skill gap metrics and data'),
('WORKFORCE_PROFILE_VIEW', 'View employee/engineer profiles'),
('WORKFORCE_PROFILE_UPDATE', 'Edit or update employee/engineer profiles'),
('WORKFORCE_HRIS_IMPORT', 'Perform bulk CSV import of internal employees'),
('WORKFORCE_REPORT_EXPORT', 'Export the bench reports as CSV'),
('DEMAND_STATUS_TRANSITION', 'Demand Status Transitions')
ON CONFLICT (name) DO NOTHING;

-- 2) Assign scopes to RM role
INSERT INTO role_scopes (role_id, scope_id)
SELECT r.id, s.id
FROM roles r
JOIN scopes s ON s.name IN (
  'WORKFORCE_NOMINATION_CREATE',
  'WORKFORCE_NOMINATION_VIEW',
  'WORKFORCE_BENCH_SEARCH',
  'WORKFORCE_ANALYTICS_VIEW',
  'WORKFORCE_SKILLGAP_VIEW',
  'WORKFORCE_SKILLGAP_REFRESH',
  'WORKFORCE_PROFILE_VIEW',
  'WORKFORCE_PROFILE_UPDATE',
  'WORKFORCE_HRIS_IMPORT',
  'WORKFORCE_REPORT_EXPORT',
  'DEMAND_STATUS_TRANSITION'
)
WHERE r.name = 'RM'
ON CONFLICT DO NOTHING;
```

---

## 7) SQL setup for test user

Create (or update) test user and map it to RM role:

```sql
-- Optional: ensure pgcrypto extension exists for crypt()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Upsert user
INSERT INTO users (username, email, password, enabled, account_locked, failed_attempts, created_at, updated_at)
VALUES ('test', 'test@gmail.com', crypt('Pass@123', gen_salt('bf')), true, false, 0, now(), now())
ON CONFLICT (email) DO UPDATE
SET username = EXCLUDED.username,
    password = EXCLUDED.password,
    enabled = true,
    account_locked = false,
    failed_attempts = 0,
    lock_time = NULL,
    updated_at = now();

-- Assign RM role to test user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'RM'
WHERE u.email = 'test@gmail.com'
ON CONFLICT DO NOTHING;
```

Verify:

```sql
SELECT id, email, enabled, account_locked FROM users WHERE email = 'test@gmail.com';
SELECT u.email, r.name AS role_name
FROM user_roles ur
JOIN users u ON u.id = ur.user_id
JOIN roles r ON r.id = ur.role_id
WHERE u.email = 'test@gmail.com';
```

---

## 8) Generate JWT token (login)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@gmail.com","password":"Pass@123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

echo "$TOKEN"
```

If token is empty, check auth-service logs and user credentials.

---

## 9) Where to paste JWT token

## 9.1 Swagger UI (recommended for manual API testing)

1. Open `http://localhost:8091/swagger-ui/index.html`
2. Click **Authorize** button (top-right)
3. Paste:

```text
Bearer <your_token_here>
```

4. Click **Authorize**, then **Close**
5. Execute protected endpoints

If you do not authorize, protected endpoints return `403`/`401`.

## 9.2 Postman

1. Open request
2. Go to **Authorization** tab
3. Type: **Bearer Token**
4. Paste only token value (without word Bearer in token field)
5. Send request

## 9.3 Curl

```bash
curl -i "http://localhost:8091/api/v1/rmg/demands?status=INTERNAL_SEARCH&page=0&size=5" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 10) End-to-end verification checklist

### 10.1 Security behavior

- Without token -> protected APIs blocked (`401`/`403`)
- With valid token and scope -> API proceeds (`200`/`201` or business validation `4xx`)
- Expired/invalid token -> `401`

### 10.2 Core test APIs

Get demands by status:

```bash
curl -i "http://localhost:8091/api/v1/rmg/demands?status=APPROVED&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

Update demand status:

```bash
DEMAND_ID=2
curl -i -X PATCH "http://localhost:8091/api/v1/rmg/demands/$DEMAND_ID/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"OPEN_EXTERNAL","comments":"transition test"}'
```

Nominate engineer (example):

```bash
curl -i -X POST "http://localhost:8091/api/v1/rmg/nominations" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": 1,
    "demandId": 2,
    "nominatedBy": 4,
    "nominationReasonByRmg": "Manual nomination for testing",
    "allocationPercentage": 50,
    "notes": "test"
  }'
```

---

## 11) Common issues and fixes

- `403` in Swagger: token not set in Authorize dialog.
- `JWT signature does not match`: `JWT_SECRET` differs between auth and workforce.
- `500` with embedded downstream `400`: business transition rejected by demand-service (invalid status transition).
- Empty demand list: valid call but no matching demand data in demand-service database.
- Token works in curl but not in Swagger: token expired or pasted without `Bearer ` prefix in Swagger Authorize.

---

## 12) Quick health URLs

- Workforce Swagger: `http://localhost:8091/swagger-ui/index.html`
- Workforce OpenAPI: `http://localhost:8091/v3/api-docs`
- Auth Swagger: `http://localhost:8080/swagger-ui/index.html` (if enabled)

