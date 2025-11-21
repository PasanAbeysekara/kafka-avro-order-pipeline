# Kafka Avro Order Pipeline

> A real-time order processing system built with Apache Kafka, Spring Boot, and Avro serialization, featuring automatic retry logic, dead letter queue handling, and a live terminal-style monitoring dashboard.

![Terminal Dashboard](./docs/terminal-ui-preview.png)

## What is This?

This is a **event streaming pipeline** that demonstrates Kafka patterns for order processing. Orders are produced with random products and prices, consumed from Kafka topics, and processed with resilient error handling. Failed messages are automatically retried and eventually moved to a Dead Letter Queue (DLQ) if they continue to fail.

The system includes:
- **Real-time monitoring dashboard** with terminal/console aesthetics
- **WebSocket streaming** for live updates
- **Avro serialization** for efficient, schema-based message encoding
- **Automatic retry mechanism** with configurable backoff
- **Dead Letter Queue** for handling permanent failures
- **Live analytics** including running average price calculations

## How It Works

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          KAFKA ORDER PIPELINE                            │
└──────────────────────────────────────────────────────────────────────────┘

    ┌─────────────┐                                    ┌──────────────┐
    │   Browser   │────────── WebSocket ──────────────▶│  Dashboard   │
    │  Dashboard  │◀────────  (STOMP)   ──────────────│   Updates    │
    └──────┬──────┘                                    └──────────────┘
           │                                                   ▲
           │ REST API                                          │
           │ POST /api/orders                                  │
           ▼                                                   │
    ┌─────────────┐                                            │
    │   Spring    │                                            │
    │    Boot     │                                            │
    │ Application │                                            │
    └──────┬──────┘                                            │
           │                                                   │
           ▼                                                   │
    ┌─────────────┐         ┌──────────────┐                   │
    │   Order     │ Avro    │    Kafka     │                   │
    │  Producer   │────────▶│ orders topic │                  │
    └─────────────┘ Binary  └──────┬───────┘                   │
                                   │                           │
                                   │ Consume                   │
                                   ▼                           │
                            ┌──────────────┐                   │
                            │    Order     │                   │
                            │   Consumer   │                   │
                            └──────┬───────┘                   │
                                   │                           │
                        ┌──────────┴──────────┐                │
                        │                     │                │
                   SUCCESS (90%)        FAILURE (10%)          │
                        │                     │                │
                        ▼                     ▼                │
                ┌───────────────┐    ┌────────────────┐        │
                │  Statistics   │    │ Retry Logic    │        │
                │    Service    │    │ (Max 3 times)  │        │
                └───────┬───────┘    └────────┬───────┘        │
                        │                     │                │
                        │              Still Failing?          │
                        │                     │                │
                        │                     ▼                │
                        │            ┌────────────────┐        │
                        │            │  orders-dlq    │        │
                        │            │     topic      │        │
                        │            └────────────────┘        │
                        │                                      │
                        └────────── WebSocket Broadcast ───────┘
                              (Orders, Stats, DLQ updates)
```

### Message Flow

1. **Order Creation**: REST API or dashboard button creates an order with random product and price ($10-$1000)
2. **Kafka Producer**: Order is serialized using Avro schema and sent to `orders` topic
3. **Consumer Processing**: Consumer receives message and processes it (10% simulated failure rate)
4. **Retry Logic**: On failure, message is retried up to 3 times with 5-second backoff
5. **Dead Letter Queue**: After max retries, message moves to `orders-dlq` topic
6. **Real-time Updates**: All events broadcast via WebSocket to connected dashboards
7. **Analytics**: Running statistics calculated and displayed in real-time

## Quick Start

```bash
# 1. Start Kafka infrastructure
docker-compose up -d

# 2. Build and run application
./run.sh

# 3. Open dashboard
open http://localhost:8080
```

### Manual Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/kafka-avro-order-pipeline-1.0.0.jar
```

## API Endpoints

```bash
# Create an order
curl -X POST http://localhost:8080/api/orders

# Get statistics
curl http://localhost:8080/api/stats

# Get recent orders
curl http://localhost:8080/api/orders/recent

# Get DLQ messages
curl http://localhost:8080/api/orders/dlq
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| **Backend** | Spring Boot 3.1.5, Java 17 |
| **Message Broker** | Apache Kafka 7.5.0 |
| **Serialization** | Apache Avro 1.11.3 |
| **Real-time Updates** | WebSocket (STOMP) |
| **Frontend** | HTML5, JavaScript, Chart.js |
| **Build Tool** | Maven 3.6+ |
| **Container** | Docker Compose |

## Avro Schema

```json
{
  "namespace": "com.example.kafka.avro",
  "type": "record",
  "name": "Order",
  "fields": [
    {"name": "orderId", "type": "string"},
    {"name": "product", "type": "string"},
    {"name": "price", "type": "float"}
  ]
}
```

## Key Features

**Event-Driven Architecture**: Asynchronous processing via Kafka topics  
**Schema Evolution**: Type-safe Avro serialization with backward compatibility  
**Fault Tolerance**: Automatic retry with exponential backoff  
**Error Handling**: Dead Letter Queue for permanent failures  
**Real-time Analytics**: O(1) running average calculation  
**Live Monitoring**: WebSocket-powered dashboard with no polling  
**Production Patterns**: Proper separation of concerns and testability  

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
kafka:
  topics:
    orders: orders              # Main topic
    orders-retry: orders-retry  # Retry topic
    orders-dlq: orders-dlq      # Dead letter queue
  retry:
    max-attempts: 3             # Retry attempts
    backoff-ms: 5000            # Retry delay
```

## Documentation

- [Implementation Details](./IMPLEMENTATION.md) - Deep dive into architecture
- [Quick Start Guide](./QUICKSTART.md) - Get running in 5 minutes
- [Testing Guide](./TESTING.md) - Test scenarios and validation
