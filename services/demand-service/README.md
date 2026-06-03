# Demand Service

The Demand Service is responsible for managing the lifecycle, validation, and analytics of talent demand within the TalentGrid platform.

## Features
- **Demand CRUD**: Core operations to create, read, update, and delete demand records.
- **Workflow & State Machine**: Structured state transitions for demand requests.
- **Integration**: Communication with Workforce Service, Matching Service, and Notification Service via Feign clients.
- **Messaging**: Kafka producers and consumers for event-driven flows.
- **Security**: RBAC, data scoping, and access control configuration.

## Directory Structure
Refer to `demand-folder-structure.txt` for details.
