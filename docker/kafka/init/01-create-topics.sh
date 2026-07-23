#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:19092}"

echo "Waiting for Kafka at ${BOOTSTRAP_SERVER}..."

until /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --list > /dev/null 2>&1; do
  sleep 2
done

echo "Kafka is ready. Creating topics..."

create_topic() {
  local topic_name="$1"
  local partitions="$2"
  local replication_factor="$3"

  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --create \
    --if-not-exists \
    --topic "${topic_name}" \
    --partitions "${partitions}" \
    --replication-factor "${replication_factor}"
}

create_topic "loan.application.created" 3 1
create_topic "loan.assessment.completed" 3 1
create_topic "loan.offer.generated" 3 1

echo "Kafka topics are ready."