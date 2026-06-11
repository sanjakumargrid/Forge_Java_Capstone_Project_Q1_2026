# TalentGrid Notification Service – Week 2 Deliverable

## Overview

This project implements the Notification Service for the TalentGrid Workforce Management Platform using an event-driven microservices architecture.

The service consumes Kafka events, persists in-app notifications into PostgreSQL, tracks notification status, and supports email notifications through Spring Mail.

---

# Features Implemented

### Kafka Integration

* Kafka Producer and Consumer implementation
* Shared event contracts and reusable Kafka library
* Asynchronous event-driven communication

### In-App Notifications

* Persist notifications in PostgreSQL
* Retrieve notifications by user
* Retrieve unread notification count
* Mark notifications as read

### Email Notifications

* Email notification channel implementation
* Spring Mail integration
* SMTP-based email delivery support

### Monitoring

* Spring Boot Actuator
* Health Checks
* Structured Logging

---

# Technology Stack

* Java 17
* Spring Boot 3
* Spring Data JPA
* PostgreSQL
* Apache Kafka
* Spring Mail
* Docker
* Maven
* Lombok
* Jackson
* Spring Boot Actuator

---

# Architecture

```text
Demand Service
      │
      ▼
demand-events
      │
      ▼
Apache Kafka
      │
      ▼
Notification Service
      │
 ┌────┴────┐
 ▼         ▼
In-App    Email
Channel   Channel
 │
 ▼
PostgreSQL
```

---

# Security Notice

For security reasons, the following credentials have been removed from the repository:

* Email Address
* SMTP Username
* SMTP Password
* Gmail App Password

To test email notifications, configure your own SMTP credentials.

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<your-email>
MAIL_PASSWORD=<your-app-password>

NOTIFICATION_EMAIL_ENABLED=true
```

If email credentials are not configured, In-App Notifications will continue to function normally.

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
./mvnw clean install -DskipTests
```

---

## Step 3 – Start Notification Service

```bash
./mvnw spring-boot:run -pl talentgrid-notification-service
```

Runs on:

```text
http://localhost:8085
```

Verify:

```bash
curl http://localhost:8085/actuator/health
```

---

## Step 4 – Start Demand Service

```bash
./mvnw spring-boot:run -pl services/demand-service
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
demand-events
notification.send
```

---

# Testing Demand Creation

Create a demand through Demand Service:

```bash
curl -X POST http://localhost:8081/api/demands \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: test-001" \
  -d '{
    "title": "Email Test",
    "location": "Chennai",
    "level": "Junior",
    "skills": ["Java"],
    "raisedBy": "101",
    "recipientEmail": "your-email@example.com"
  }'
```

Expected Response:

```text
✅ DEMAND_CREATED event published to Kafka.
Check Kafka UI at http://localhost:7777
```

---

# Verify Kafka Events

Open Kafka UI:

```text
http://localhost:7777
```

Verify:

### demand-events

A new `DEMAND_CREATED` event is published.

### notification.send

A notification event is generated for downstream notification processing.

---

# Verify In-App Notifications

## Get Notifications

```bash
curl "http://localhost:8085/api/notifications?userId=101"
```

---

## Get Unread Count

```bash
curl "http://localhost:8085/api/notifications/count?userId=101"
```

Example:

```json
{
  "unreadCount": 13
}
```

---

## Mark Notification as Read

```bash
curl -X PATCH \
"http://localhost:8085/api/notifications/14/read?userId=101"
```

---

## Verify Updated Count

```bash
curl "http://localhost:8085/api/notifications/count?userId=101"
```

Example:

```json
{
  "unreadCount": 12
}
```

---

# PostgreSQL Verification

Connect to PostgreSQL:

```bash
docker exec -it talentgrid-postgres psql -U talentgrid -d talentgrid
```

View notifications:

```sql
SELECT id,user_id,title,is_read,created_at
FROM notifications
ORDER BY id;
```

Verify:

* Notifications are persisted.
* Read status updates correctly.
* Notification count matches API responses.

---

# Email Notification Testing

To test email notifications:

1. Enable email delivery:

```properties
NOTIFICATION_EMAIL_ENABLED=true
```

2. Configure valid SMTP credentials:

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<your-email>
MAIL_PASSWORD=<your-app-password>
```

3. Replace:

```json
"recipientEmail": "your-email@example.com"
```

with a valid email address.

> **Note:** Email credentials have been intentionally removed from the repository before submission. Reviewers should use their own credentials for testing email functionality.

---

# Verification Completed

The following functionalities have been successfully tested:

✅ Kafka Producer

✅ Kafka Consumer

✅ PostgreSQL Notification Persistence

✅ Notification Retrieval API

✅ Unread Notification Count API

✅ Mark Notification as Read API

✅ In-App Notification Lifecycle

✅ Health Monitoring via Actuator

---

# Deliverable Summary

This implementation demonstrates a production-style Notification Service built using Spring Boot, Apache Kafka, and PostgreSQL. The solution supports asynchronous event processing, notification persistence, notification lifecycle management, and configurable email delivery through a scalable event-driven architecture.
