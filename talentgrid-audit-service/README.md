# TalentGrid Audit Service – Week 2 Deliverable

## Overview

This project implements the centralized Audit Logging Service for the TalentGrid Workforce Management Platform using an event-driven microservices architecture.

The solution consists of:

* Audit Service
* Audit Client Library
* Kafka-based event communication
* PostgreSQL audit persistence

Any TalentGrid service can publish audit events through the Audit Client Library. The Audit Service consumes these events from Kafka and stores a complete audit trail in PostgreSQL.

---

# Features Implemented

## Audit Client Library

Reusable audit library shared across TalentGrid services.

Provides:

* AuditLogClient
* AuditAction enum
* AuditLogPayload DTO
* Standardized audit event publishing

Supports audit logging for:

* CREATE
* UPDATE
* DELETE
* APPROVE
* REJECT
* ASSIGN
* UNASSIGN
* LOGIN
* LOGOUT
* STATUS_CHANGE

---

## Kafka Integration

* Kafka Producer implementation
* Kafka Consumer implementation
* Shared BaseEvent contract
* Event-driven audit processing
* Reliable asynchronous communication

Topic Used:

```text
system-events
```

---

## Audit Logging

Stores complete audit history including:

* Entity Type
* Entity ID
* Action
* Actor ID
* Before State
* After State
* Service Name
* Endpoint
* IP Address
* User Agent
* Timestamp

---

## PostgreSQL Persistence

Audit events are persisted into:

```text
audit_logs
```

Supports PostgreSQL JSONB storage for:

```json
{
  "title": "Senior Java Developer",
  "status": "OPEN"
}
```

---

## Monitoring

* Structured Logging
* Kafka Consumer Logging
* Audit Persistence Logging

---

# Technology Stack

* Java 17
* Spring Boot 3
* Spring Data JPA
* PostgreSQL
* Apache Kafka
* Docker
* Maven
* Lombok
* Jackson
* Hibernate 6

---

# Architecture

```text
Demand Service
       │
       ▼
AuditLogClient
       │
       ▼
system-events
       │
       ▼
Apache Kafka
       │
       ▼
Audit Service
       │
       ▼
PostgreSQL
(audit_logs)
```

Any TalentGrid service can publish audit events through the shared Audit Client Library.

---

# Prerequisites

Install:

* Java 17+
* Maven 3.9+
* Docker Desktop

Verify:

```bash
java -version
mvn -version
docker --version
```

---

# Running the Project

## Step 1 – Start Infrastructure

```bash
docker compose up -d
```

Verify:

```bash
docker ps
```

Expected Containers:

```text
talentgrid-kafka-broker
talentgrid-kafka-ui
talentgrid-postgres
```

---

## Step 2 – Build Project

```bash
mvn clean install
```

---

## Step 3 – Start Audit Service

```bash
cd services/talentgrid-audit-service
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8085
```

---

## Step 4 – Start Demand Service

```bash
cd services/demand-service
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8081
```

---

# Kafka UI

Access:

```text
http://localhost:7777
```

Available Topics:

```text
system-events
```

---

# Testing Audit Logging

Create a Demand using Postman:

```http
POST http://localhost:8081/api/v1/demands
```

Request Body:

```json
{
  "title": "Senior Java Developer",
  "location": "Chennai",
  "level": "Senior",
  "skills": ["Java", "Spring Boot", "Kafka"],
  "raisedBy": "Manager",
  "recipientEmail": "test@example.com"
}
```

Expected Response:

```text
DEMAND_CREATED event published to Kafka
```

---

# Verify Kafka Events

Open Kafka UI:

```text
http://localhost:7777
```

Verify:

```text
system-events
```

A new audit event should be published.

Example Event:

```text
AUDIT_LOG_ENTRY
```

---

# Verify Audit Service Logs

Expected Logs:

```text
Received audit event
Audit log persisted successfully
Audit event processed successfully
```

---

# PostgreSQL Verification

Connect to PostgreSQL:

```bash
psql postgres
```

Select database:

```sql
\c talentgrid
```

View audit logs:

```sql
SELECT *
FROM audit_logs
ORDER BY id;
```

Verify:

* Audit records are persisted.
* Audit actions are stored.
* JSONB data is stored correctly.
* Metadata is captured correctly.

---

# JSONB Verification

Run:

```sql
SELECT
    id,
    after_state->>'title' AS title,
    after_state->>'status' AS status
FROM audit_logs;
```

Example:

```text
 id |         title         | status
----+-----------------------+--------
  1 | Senior Java Developer | OPEN
  2 | Backend Developer     | OPEN
```

---

# Kafka Durability Test

1. Stop Audit Service.
2. Create a new Demand.
3. Verify no new row exists in audit_logs.
4. Start Audit Service.
5. Verify the pending Kafka event is consumed.

Expected Result:

* Event is retained in Kafka.
* Audit Service processes event after restart.
* Audit record is inserted successfully.

---

# Verification Completed

The following functionalities have been successfully tested:

✅ Audit Client Library

✅ Kafka Producer

✅ Kafka Consumer

✅ PostgreSQL Audit Persistence

✅ JSONB Storage

✅ Audit Event Processing

✅ Demand Service Integration

✅ Kafka Durability

✅ End-to-End Audit Flow

---

# Deliverable Summary

This implementation demonstrates a production-style centralized Audit Logging solution built using Spring Boot, Apache Kafka, and PostgreSQL.

The Audit Client Library enables all TalentGrid services to publish standardized audit events, while the Audit Service provides reliable persistence, audit history tracking, JSONB support, and event-driven processing through Kafka.

The solution ensures a complete audit trail for critical business operations across the TalentGrid platform.
