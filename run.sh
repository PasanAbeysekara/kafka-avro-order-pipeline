#!/bin/bash

echo "===================================="
echo "Kafka Order Pipeline - Quick Start"
echo "===================================="
echo ""

# Check if Kafka is running
if ! docker ps | grep -q kafka; then
    echo "Starting Kafka infrastructure..."
    docker-compose up -d
    echo "Waiting for Kafka to be ready (30 seconds)..."
    sleep 30
else
    echo "Kafka is already running ✓"
fi

# Check if JAR exists
if [ ! -f "target/kafka-avro-order-pipeline-1.0.0.jar" ]; then
    echo ""
    echo "Building application..."
    mvn clean package -DskipTests
fi

echo ""
echo "Starting application..."
echo "Dashboard will be available at: http://localhost:8080"
echo ""
echo "Press Ctrl+C to stop"
echo ""

java -jar target/kafka-avro-order-pipeline-1.0.0.jar
