# TalentGrid Demand Service

The **Demand Service** is a core microservice in the TalentGrid ecosystem responsible for the complete lifecycle management of workforce demands. It handles everything from initial demand creation (Draft) to final fulfillment, tracking internal bench assignments and external recruitment metrics.

This service is built with Spring Boot, utilizes PostgreSQL for robust relational data persistence, and integrates with Apache Kafka for asynchronous event broadcasting across the enterprise ecosystem.

---

## 🏗️ Architecture & Stack

- **Framework**: Spring Boot 3.5.x / Java 17
- **Database**: PostgreSQL (JPA / Hibernate)
- **Messaging**: Apache Kafka (Event-driven architecture)
- **Security**: Stateless JWT Authentication with `@PreAuthorize` role enforcement
- **Inter-service Communication**: OpenFeign (`user-auth-service`, `notification-client`, `audit-client`)

---

## 🚀 Getting Started

### Prerequisites
- **Java 17**
- **Apache Maven** (or use the provided `mvnw` wrapper)
- **PostgreSQL** running locally on port `5432`
- **Apache Kafka & Zookeeper** running locally on port `9092`

### Building (monorepo)

This module depends on **`talentgrid-shared`**, **`talentgrid-kafka`**, and client jars from the parent reactor. From the **repository root** (`Forge_Java_Capstone_Project_Q1_2026_Services`), run:

```bash
mvn clean install -DskipTests
```

Then you can build or test inside `services/demand-service` alone. Running `mvn compile` only inside `user-auth-service` or `demand-service` **before** the root install can fail with missing classes (stale local `~/.m2` artifacts).

### Running the Application Locally

1. **Start Infrastructure (Docker)**:
   Ensure your PostgreSQL and Kafka containers are running via the root `docker-compose.yml`.

   *Note: Ensure your `application.yml` database credentials are set to match these values.*

2. **Build and Run the Service**:
   Navigate to the `demand-service` directory and execute:
   ```bash
   mvn clean spring-boot:run
   ```
   Or if memory constrained:
   ```bash
   export MAVEN_OPTS="-Xmx512m" && mvn clean spring-boot:run
   ```

The service will start on `http://localhost:8082`.

---

## 🔄 Demand Lifecycle State Machine

Canonical statuses: `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `INTERNAL_SEARCH`, `OPEN_EXTERNAL`, `FILLED`, `ON_HOLD`, `CLOSED`. Withdraw / duplicate flows use `CLOSED` with an appropriate `closure_reason` (see `ClosureReason` enum).

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> PENDING_APPROVAL : HM submit
    DRAFT --> APPROVED : PM submit auto
    PENDING_APPROVAL --> APPROVED : PM approve
    PENDING_APPROVAL --> CLOSED : PM reject or 72h SLA
    APPROVED --> INTERNAL_SEARCH : default
    APPROVED --> OPEN_EXTERNAL : bench hiring
    INTERNAL_SEARCH --> OPEN_EXTERNAL : no match or HM rejects nomination
    INTERNAL_SEARCH --> FILLED : HM accepts nomination
    INTERNAL_SEARCH --> ON_HOLD : RM hold
    OPEN_EXTERNAL --> FILLED : TA offer approve
    OPEN_EXTERNAL --> ON_HOLD : Recruiter hold
    ON_HOLD --> INTERNAL_SEARCH : resume
    ON_HOLD --> OPEN_EXTERNAL : resume
    ON_HOLD --> CLOSED : RM close
    FILLED --> CLOSED : auto
```

### Transition matrix (summary)

| From | To |
|------|-----|
| `DRAFT` | `PENDING_APPROVAL`, `APPROVED` (PM auto) |
| `PENDING_APPROVAL` | `APPROVED`, `CLOSED` |
| `APPROVED` | `INTERNAL_SEARCH`, `OPEN_EXTERNAL` (bench) |
| `INTERNAL_SEARCH` | `OPEN_EXTERNAL`, `FILLED`, `ON_HOLD` |
| `OPEN_EXTERNAL` | `FILLED`, `ON_HOLD` |
| `FILLED` | `CLOSED` (chained auto-close after fill) |
| `ON_HOLD` | `INTERNAL_SEARCH`, `OPEN_EXTERNAL`, `CLOSED` |

`INTERNAL_SEARCH` → `OPEN_EXTERNAL` is allowed when `closure_reason` is `NO_INTERNAL_MATCH` or `HM_REJECTED_NOMINATION`, or after the internal-search business-day gate (see `TransitionValidator`).

### RBAC: JWT roles and scopes

Issue tokens from **user-auth-service** with the same **scope names** as in `talentgrid-api-gateway-service` `rbac-rules.yml`. Demand routes use authorities such as:

- `DEMAND_VIEW`, `DEMAND_CREATE`, `DEMAND_UPDATE`, `DEMAND_DELETE`
- `DEMAND_PM_APPROVE` (approve/reject while `PENDING_APPROVAL` — **only** the PM for the demand's project; also PM submit-from-draft)
- `DEMAND_STATUS_TRANSITION` (PATCH status), `DEMAND_PIPELINE_VIEW`, `DEMAND_SUBMIT` (POST submit)
- `DEMAND_NOMINATE`, `DEMAND_HM_NOMINATION_DECIDE` (internal nominations)

Role names used in code checks: `ADMIN`, `PORTFOLIO_MANAGER` (legacy alias `PROJECT_MANAGER`), **`HIRING_MANAGER`** (alias `HM`), `RESOURCE_MANAGER`, `RM`, `RMG` (treated as RM for lifecycle), `RECRUITER`, `TA_MANAGER`, `EMPLOYEE`.

> **Note:** Illegal transitions return **HTTP 400** with validation errors from `TransitionValidator` / `DemandStateMachine`.

---

## ⚖️ Core Business Rules

1. **Internal search gate (`INTERNAL_SEARCH` → `OPEN_EXTERNAL`)**:
   Blocked until the configured business-day window unless `closure_reason` is `NO_INTERNAL_MATCH` or `HM_REJECTED_NOMINATION` (see `TransitionValidator`).

2. **ON_HOLD resume logic**:
   When a demand enters `ON_HOLD`, `previousStatus` is stored. Resume transitions must return to that previous active state.

3. **Closure reason validation**:
   `TransitionValidator` enforces required `closure_reason` values for targets such as `FILLED` and `CLOSED` where applicable.

4. **Approval SLA**:
   `ApprovalSlaScheduler` sends a 24h reminder, then after 72h transitions the demand to `CLOSED` with `SLA_APPROVAL_BREACH` and publishes `DEMAND_APPROVAL_SLA_CLOSED`.

5. **Internal nominations**:
   RM creates nominations in `INTERNAL_SEARCH`; HM (demand owner) accepts (`FILLED` → auto `CLOSED`) or rejects (transition to `OPEN_EXTERNAL` with `HM_REJECTED_NOMINATION`).

---

## 🌐 REST API Endpoints

### Demand CRUD

| Method | Endpoint | Description |
|:---|:---|:---|
| `GET` | `/api/v1/demands` | Enterprise demand search (Filters: status, priority, business unit, etc.) |
| `POST` | `/api/v1/demands` | Create a new workforce demand. Status initializes as `DRAFT`. |
| `GET` | `/api/v1/demands/{id}` | Fetch granular details for a specific demand. |
| `PATCH`| `/api/v1/demands/{id}` | Update editable fields. **Restricted to DRAFT state only.** |
| `DELETE`| `/api/v1/demands/{id}`| Soft delete (`is_deleted = true`). **Restricted to DRAFT state only.** |

### Demand Workflow & Lifecycle

| Method | Endpoint | Description |
|:---|:---|:---|
| `POST` | `/api/v1/demands/{id}/submit` | HM submits for PM approval, or PM auto-approves from `DRAFT`. |
| `POST` | `/api/v1/demands/{id}/approve` | Project **PM** for that demand's `projectId` only (`DEMAND_PM_APPROVE`); same checks as `PUT /api/v1/project-manager/demands/{id}/approve`. |
| `PATCH` | `/api/v1/demands/{id}/status` | Trigger a state transition. Expects `targetStatus` and optional `comments`/`closureReason`. |
| `GET` | `/api/v1/demands/{id}/pipeline` | Retrieves the real-time split of internal/external recruiting counts and status audit trails. |
| `GET` | `/api/v1/demands/{id}/history` | Retrieves the chronological timeline (audit trail) of status modifications. |
| `POST` | `/api/v1/demands/{id}/nominations` | RM creates an internal nomination (INTERNAL_SEARCH only). |
| `POST` | `/api/v1/demands/{id}/nominations/{nid}/hm-decision` | HM accepts or rejects a nomination (`accept` query param). |

### Analytics

| Method | Endpoint | Description |
|:---|:---|:---|
| `GET` | `/api/v1/analytics/demands` | Comprehensive analytics: total demands, active searches, fill rates, avg time-to-fill, and internal vs. external ratios. |

---

## 📡 Kafka Events

The service acts as a producer, broadcasting real-time asynchronous events upon key lifecycle milestones. All events are published to the unified `demand-events` Kafka topic with the demand ID as the routing key, ensuring strict partition affinity and chronological ordering.

| Event Type | Trigger Condition |
|:---|:---|
| `DEMAND_CREATED` | A new demand is initially created in `DRAFT` status. |
| `DEMAND_SUBMITTED` | Demand submitted from draft (HM path). |
| `DEMAND_PENDING_APPROVAL` | Demand entered `PENDING_APPROVAL` (HM submit). |
| `DEMAND_APPROVAL_REMINDER` | 24h SLA reminder while pending PM approval. |
| `DEMAND_APPROVAL_SLA_CLOSED` | 72h SLA auto-close to `CLOSED` (`SLA_APPROVAL_BREACH`). |
| `DEMAND_APPROVED` | Approved; cascades toward `INTERNAL_SEARCH` or bench `OPEN_EXTERNAL`. |
| `DEMAND_EXTERNAL_OPENED` | External hiring opened. |
| `DEMAND_FILLED` | Unified fill event before auto-close (`fillType` / `closureReason` distinguish channel). |
| `DEMAND_ON_HOLD` | Demand is placed `ON_HOLD`. |
| `DEMAND_RESUMED` | Demand is resumed from `ON_HOLD` to its previous active state. |
| `DEMAND_CLOSED` | Terminal `CLOSED` (including auto-close after fill). |
| `DEMAND_INTERNAL_NOMINATION_PENDING_HM` | RM nominated an internal candidate; HM must decide. |


# TalentGrid Demand & Notification Lifecycle Verification Guide

---

## 📌 Project Overview

This platform orchestrates enterprise workforce hiring requests (**Demands**) across a decoupled microservices architecture. It utilizes **Apache Kafka** as an asynchronous event-driven backbone to safely broadcast state machine transitions between core services.

```
[Client / Curl] 
       │
       ▼
[demand-service] ──(Kafka: demand-events Topic)──► [DemandEventTranslator]
                                                          │
                                                (Kafka: notification.send)
                                                          │
                                                          ▼
[Real Inbox / Email] ◄────────────────────────── [notification-service]

```

When an event occurs (such as submitting or approving a demand), **`demand-service`** broadcasts an immutable lifecycle token over Kafka. An internal listener catches the message, processes business mapping rules, and commands the **`notification-service`** to dynamically render rich HTML email templates and route them to real inboxes.

---

## 🛠️ Prerequisites

* **Java:** Version 21 or 25


* **Local Database:** PostgreSQL initialized and running locally on port `5432`

* **Kafka Broker:** Apache Kafka cluster running on port `9092`


---

## 🚀 Running the Applications

### 1. Build the Project Ecosystem

From the root project directory, compile and package all modules and shared libraries:

```bash
mvn clean install -DskipTests

```

### 2. Configure Mailroom Properties (Crucial for Real Email Delivery)

Before launching the services, you must equip the notification daemon with mail server credentials. Open the properties file located at `talentgrid-notification-service/src/main/resources/application.properties` and add your account details:

```properties
spring.mail.username=your-gmail@gmail.com
spring.mail.password=xxxx-xxxx-xxxx-xxxx   # Your 16-character Google App Password (NOT your login password)
notification.email.from=your-gmail@gmail.com

```

> **💡 Generation Tip:** You can quickly generate a secure 16-character App Password by visiting your account security dashboard at [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).
>
>
> If you ever need to temporarily mute email routing during offline runs, set `notification.email.enabled=false`.
>
>

### 3. Start the Notification Service

Navigate to the notification module directory and run it:

```bash
cd talentgrid-notification-service
mvn spring-boot:run

```

> **Expected Status:** Confirm console outputs `Started NotificationServiceApplication on port 8085` and is listening to group `notification-service-group`.
>
>

### 4. Start the Demand Service

Open a fresh terminal window, navigate to the demand service module directory, and run it:

```bash
cd services/demand-service
mvn spring-boot:run

```

> **Expected Status:** Confirm console outputs `Started DemandServiceApplication on port 8082` and logs `Subscribed to topic(s): demand-events`.
>
>

---

## 🧪 End-to-End Foolproof Verification Sequence

These single-line terminal commands ensure your shell parses the JSON string seamlessly without whitespace or newline failures.

### Step 1: Create a Fresh Draft Demand

This maps strictly to your service's data contract constraints (`clientInterview`, `businessUnit`, `budget`, `targetDate`) and specific granular enterprise grade enum definitions (`T3_SENIOR`, `FULL_TIME`).

```bash
curl -X POST http://localhost:8082/api/v1/demands -H "Content-Type: application/json" -d '{"title": "Senior Java Developer", "description": "Experienced Java/Spring Boot developer needed for TalentGrid platform", "level": "T3_SENIOR", "skills": ["Java", "Spring Boot", "Kafka", "PostgreSQL"], "location": "Chennai", "workMode": "HYBRID", "experience": 5, "department": "Engineering", "employmentType": "FULL_TIME", "requiredCount": 2, "priority": "HIGH", "accountId": 1, "projectId": 1, "businessUnit": "Enterprise Applications", "budget": 150000.00, "targetDate": "2026-08-31", "clientInterview": true}'

```

* **Expected Output:** Returns a `201 Created` payload with `"status":"DRAFT"`.


* **Action Item:** Identify and look at the value assigned to **`"demandId"`** in the JSON output block (Assume it is **`3`** for the steps below).

---

### Step 2: Route local event alerts to your corporate email

Because local test profiles secure authentication via a default mock session (`creator@griddynamics.com`), you must execute a brief database routing override so the Kafka workers find your actual mailbox.

Log into your local PostgreSQL database client tool or native terminal container and run:

```sql
UPDATE demands SET creator_email = 'your-email@griddynamics.com' WHERE demand_id = 3;

```

*(Swap `your-email@griddynamics.com` with your actual corporate email address, and verify `demand_id` matches your step 1 response output.)*

---

### Step 3: Submit the Demand for Management Approval

Transition the demand out of a draft status using a `PATCH` payload:

```bash
curl -i -X PATCH http://localhost:8082/api/v1/demands/3/status -H "Content-Type: application/json" -d '{"targetStatus": "PENDING_APPROVAL", "comments": "Submitting verification run"}'

```

* **Expected Output:** `200 OK`.


* **📧 Verification:** Check your mailbox. You will receive an automated **Demand Submitted for Approval** layout.



---

### Step 4: Approve the Request

Enforce managerial signature clearing to release the demand to sourcing teams via a `POST` mapping:

```bash
curl -i -X POST http://localhost:8082/api/v1/demands/3/approve -H "Content-Type: application/json" -d '{"decision": "APPROVED", "comments": "Approved", "assignedRm": 101, "assignedRmName": "Rahul Mehta"}'

```

* **Expected Output:** `200 OK`. The status shifts securely to `"INTERNAL_SEARCH"`.
* **📧 Verification:** Check your mailbox. You will receive a **Demand Approved** layout stating "Approved By: Admin User".



---

### Step 5: Fast-Forward the RMG Time Gate

Your core state engine prevents exposing demands to outside staffing agencies (`OPEN_EXTERNAL`) unless the local internal workbench team has exhausted search efforts for 5 business days.

To fast-forward this restriction for your testing, run this time-travel script in your PostgreSQL instance:

```sql
UPDATE demands SET search_start_at = NOW() - INTERVAL '6 days' WHERE demand_id = 3;

```

---

### Step 6: Open the Demand to External Vendors

With the 5-day security check cleared, release the specification externally using a `PATCH` payload:

```bash
curl -i -X PATCH http://localhost:8082/api/v1/demands/3/status -H "Content-Type: application/json" -d '{"targetStatus": "OPEN_EXTERNAL", "comments": "Opening up to external agencies"}'

```

* **Expected Output:** `200 OK`.
* **📧 Verification:** Check your mailbox. You will receive an **External Hiring Opened** notification layout.



---

### Step 7: Log External Fulfillment Milestone

The state machine strictly bars moving directly from searching to archiving without intermediate fulfillment logging. Transition to the completed milestone flag first:

```bash
curl -i -X PATCH http://localhost:8082/api/v1/demands/3/status -H "Content-Type: application/json" -d '{"targetStatus": "FILLED_EXTERNAL", "closureReason": "FILLED_EXTERNAL", "comments": "All positions filled successfully via external tracking."}'

```

* **Expected Output:** `200 OK`.

---

### Step 8: Terminal Lifecycle Closure

Now that the positions have been marked as fulfilled, permanently lock and complete the lifecycle file record:

```bash
curl -i -X PATCH http://localhost:8082/api/v1/demands/3/status -H "Content-Type: application/json" -d '{"targetStatus": "CLOSED", "closureReason": "FILLED_EXTERNAL", "comments": "Closing out the demand record permanently."}'

```

* **Expected Output:** `200 OK` status showing terminal state `"status":"CLOSED"`.
* **📧 Verification:** Check your mailbox. You will receive a final **Demand Closed** data breakdown message summarizing headcount completion matrices!


**Note:**
```markdown
## 🛰️ Consuming Demand Events

Other microservices can subscribe to demand lifecycle updates by listening to the `demand-events` Kafka topic.

### 1. Add Dependency
Include the shared Kafka library module in your service's `pom.xml` file:

```xml
<dependency>
    <groupId>com.talentgrid</groupId>
    <artifactId>talentgrid-kafka</artifactId>
    <version>${project.version}</version>
</dependency>

```

### 2. Configure Properties

Add these configuration settings to your `application.properties` file. Because `add.type.headers=false` is enforced across the platform, you must explicitly declare the target deserialization wrapper class type:

```properties
spring.kafka.consumer.group-id=your-service-group
spring.json.value.default.type=com.talentgrid.kafka.events.BaseEvent

```

### 3. Implement Kafka Listener

Create a listener component to extract and handle incoming payload event packets:

```java
package com.talentgrid.yourservice.kafka;

import com.talentgrid.kafka.events.BaseEvent;
import com.talentgrid.kafka.events.demand.DemandPayload;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DemandEventSubscriber {

    @KafkaListener(topics = "demand-events", groupId = "your-service-group")
    public void onEvent(BaseEvent<DemandPayload> event) {
        String eventType = event.getEventType(); // Can be DEMAND_SUBMITTED, DEMAND_APPROVED, DEMAND_EXTERNAL_OPENED, DEMAND_CLOSED
        DemandPayload payload = event.getPayload();
        
        System.out.printf("Processed lifecycle event [%s] for Demand ID: %d%n", eventType, payload.getDemandId());
    }
}

```

```

```