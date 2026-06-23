#!/bin/bash

M2_REPO="$HOME/.m2/repository/com/talentgrid"

echo "Starting Docker Compose (Postgres & Kafka)..."
docker compose up -d

echo "Waiting for Postgres to be ready..."
until docker exec talentgrid-postgres pg_isready -U talentgrid; do
  sleep 2
done
echo "Postgres is ready!"

echo "Starting microservices from .m2..."
java -jar $M2_REPO/user-auth-service/1.0.0-SNAPSHOT/user-auth-service-1.0.0-SNAPSHOT.jar > auth.log 2>&1 &
java -jar $M2_REPO/demand-service/1.0.0-SNAPSHOT/demand-service-1.0.0-SNAPSHOT.jar > demand.log 2>&1 &
java -jar $M2_REPO/candidate-service/1.0.0-SNAPSHOT/candidate-service-1.0.0-SNAPSHOT.jar > candidate.log 2>&1 &
java -jar $M2_REPO/application-service/1.0.0-SNAPSHOT/application-service-1.0.0-SNAPSHOT.jar > application.log 2>&1 &
java -jar $M2_REPO/talentgrid-audit-service/1.0.0-SNAPSHOT/talentgrid-audit-service-1.0.0-SNAPSHOT.jar > audit.log 2>&1 &
java -jar $M2_REPO/offer-service/1.0.0-SNAPSHOT/offer-service-1.0.0-SNAPSHOT.jar > offer.log 2>&1 &
java -jar $M2_REPO/talentgrid-notification-service/1.0.0-SNAPSHOT/talentgrid-notification-service-1.0.0-SNAPSHOT.jar > notification.log 2>&1 &
java -jar $M2_REPO/interview-service/1.0.0-SNAPSHOT/interview-service-1.0.0-SNAPSHOT.jar > interview.log 2>&1 &

echo "All services launched in background. Check *.log files for startup progress."
