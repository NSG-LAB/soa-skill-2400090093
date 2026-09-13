#!/usr/bin/env bash

set -e

BASE="$(pwd)"
PRODUCER="$BASE/producer-service"
CONSUMER="$BASE/consumer-service"

KAFKA_NAME="spring-kafka"
KAFKA_IMAGE="apache/kafka:4.0.0"
TOPIC="api-communication"

echo "=============================================="
echo " SPRING BOOT + APACHE KAFKA DEMO"
echo "=============================================="

# ------------------------------------------------
# 1. Check Docker
# ------------------------------------------------
echo "[1/10] Checking Docker..."

if ! docker info >/dev/null 2>&1; then
    echo "ERROR: Docker is not running."
    exit 1
fi

echo "Docker: OK"

# ------------------------------------------------
# 2. Start Kafka
# ------------------------------------------------
echo "[2/10] Starting Kafka..."

if docker ps -a --format '{{.Names}}' | grep -qx "$KAFKA_NAME"; then
    docker start "$KAFKA_NAME" >/dev/null 2>&1 || true
else
    docker run -d \
        --name "$KAFKA_NAME" \
        -p 9092:9092 \
        -e KAFKA_NODE_ID=1 \
        -e KAFKA_PROCESS_ROLES=broker,controller \
        -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
        -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
        -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
        -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
        -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
        -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
        -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
        -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
        -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
        "$KAFKA_IMAGE"
fi

echo "Kafka container: OK"

# ------------------------------------------------
# 3. Wait for Kafka
# ------------------------------------------------
echo "[3/10] Waiting for Kafka..."

for i in {1..30}; do
    if docker exec "$KAFKA_NAME" \
        /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --list >/dev/null 2>&1; then
        echo "Kafka is ready."
        break
    fi

    sleep 2
done

# ------------------------------------------------
# 4. Create topic
# ------------------------------------------------
echo "[4/10] Creating Kafka topic..."

docker exec "$KAFKA_NAME" \
    /opt/kafka/bin/kafka-topics.sh \
    --create \
    --if-not-exists \
    --topic "$TOPIC" \
    --bootstrap-server localhost:9092 \
    --partitions 1 \
    --replication-factor 1 >/dev/null 2>&1 || true

echo "Topic: $TOPIC"

# ------------------------------------------------
# 5. Verify project
# ------------------------------------------------
echo "[5/10] Checking Spring Boot projects..."

if [ ! -f "$PRODUCER/pom.xml" ]; then
    echo "ERROR: Producer pom.xml not found."
    exit 1
fi

if [ ! -f "$CONSUMER/pom.xml" ]; then
    echo "ERROR: Consumer pom.xml not found."
    exit 1
fi

# ------------------------------------------------
# 6. Producer configuration
# ------------------------------------------------
echo "[6/10] Configuring Producer..."

mkdir -p "$PRODUCER/src/main/resources"

cat > "$PRODUCER/src/main/resources/application.properties" <<'PROPERTIES'
server.port=8081

spring.kafka.bootstrap-servers=localhost:9092

spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
PROPERTIES

# ------------------------------------------------
# 7. Build applications
# ------------------------------------------------
echo "[7/10] Building Producer..."

cd "$PRODUCER"
mvn clean package -DskipTests -q

echo "Producer build: SUCCESS"

echo "Building Consumer..."

cd "$CONSUMER"
mvn clean package -DskipTests -q

echo "Consumer build: SUCCESS"

# ------------------------------------------------
# 8. Start Consumer
# ------------------------------------------------
echo "[8/10] Starting Consumer API..."

cd "$CONSUMER"

nohup mvn spring-boot:run \
    > "$BASE/consumer.log" 2>&1 &

CONSUMER_PID=$!

echo "Consumer PID: $CONSUMER_PID"

# Wait for Consumer
echo "Waiting for Consumer..."

for i in {1..30}; do
    if curl -s http://localhost:8082 >/dev/null 2>&1; then
        break
    fi
    sleep 2
done

# ------------------------------------------------
# 9. Start Producer
# ------------------------------------------------
echo "[9/10] Starting Producer API..."

cd "$PRODUCER"

nohup mvn spring-boot:run \
    > "$BASE/producer.log" 2>&1 &

PRODUCER_PID=$!

echo "Producer PID: $PRODUCER_PID"

sleep 8

# ------------------------------------------------
# 10. Test communication
# ------------------------------------------------
echo "[10/10] Testing API → Kafka → API..."

echo ""
echo "Sending message..."
echo ""

RESPONSE=$(curl -s -X POST \
    "http://localhost:8081/api/send?message=Hello%20from%20Producer%20API")

echo "Producer Response:"
echo "$RESPONSE"

sleep 3

echo ""
echo "=============================================="
echo " CONSUMER OUTPUT"
echo "=============================================="

if [ -f "$BASE/consumer.log" ]; then
    grep -A5 -B2 "Message received" "$BASE/consumer.log" || \
    tail -30 "$BASE/consumer.log"
fi

echo ""
echo "=============================================="
echo " KAFKA COMMUNICATION COMPLETE"
echo "=============================================="

echo ""
echo "Architecture:"
echo ""
echo "REST Client"
echo "    ↓"
echo "Producer API :8081"
echo "    ↓"
echo "Spring Kafka"
echo "    ↓"
echo "Kafka :9092"
echo "    ↓"
echo "Topic: $TOPIC"
echo "    ↓"
echo "Spring Kafka"
echo "    ↓"
echo "Consumer API :8082"
echo ""

echo "Logs:"
echo "  Producer: $BASE/producer.log"
echo "  Consumer: $BASE/consumer.log"
echo ""

echo "Running processes:"
echo "  Producer PID: $PRODUCER_PID"
echo "  Consumer PID: $CONSUMER_PID"
echo ""

echo "To stop APIs:"
echo "  kill $PRODUCER_PID $CONSUMER_PID"

echo ""
echo "To stop Kafka:"
echo "  docker stop kafka"

echo "=============================================="
