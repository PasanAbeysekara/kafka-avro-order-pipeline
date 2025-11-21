# Quick Start Guide

Get up and running with the Kafka Order Pipeline in minutes!

## Prerequisites

- Java 17+ installed
- Docker and Docker Compose installed
- Maven installed (or use `./mvnw`)

## Quick Start (5 minutes)

### Step 1: Start Kafka (1 minute)

```bash
docker-compose up -d
```

Wait ~30 seconds for Kafka to be ready. Check status:

```bash
docker-compose ps
```

All services should be "Up".

### Step 2: Build the Application (2 minutes)

```bash
mvn clean package
```

### Step 3: Run the Application (1 minute)

```bash
java -jar target/kafka-avro-order-pipeline-1.0.0.jar
```

Or:

```bash
mvn spring-boot:run
```

### Step 4: Open the Dashboard

Open your browser:
```
http://localhost:8080
```

## Quick Demo

### Using the Dashboard

1. Click "Create New Order" button multiple times
2. Watch orders appear in "Recent Orders" section
3. See the average price chart update in real-time
4. Observe retry counts and DLQ messages (10% of orders will fail randomly)

### Using REST API

Create orders via curl:

```bash
# Create a single order
curl -X POST http://localhost:8080/api/orders

# Create 10 orders
for i in {1..10}; do curl -X POST http://localhost:8080/api/orders; sleep 1; done

# Get statistics
curl http://localhost:8080/api/stats

# Get recent orders
curl http://localhost:8080/api/orders/recent

# Get DLQ messages
curl http://localhost:8080/api/orders/dlq
```

## What's Happening?

1. **Order Creation**: Random orders are created with products and prices
2. **Kafka Producer**: Serializes orders using Avro and sends to Kafka
3. **Processing**: Consumer processes orders (10% fail randomly)
4. **Retry Logic**: Failed orders retry up to 3 times
5. **DLQ**: Orders that fail 3 times go to Dead Letter Queue
6. **Statistics**: Running average of prices calculated in real-time
7. **WebSocket**: Live updates pushed to dashboard

## Stopping

```bash
# Stop application
Ctrl+C

# Stop Kafka
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Troubleshooting

### Port 8080 Already in Use

Change the port in `src/main/resources/application.yml`:

```yaml
server:
  port: 9090
```

### Kafka Connection Failed

Ensure Kafka is running:

```bash
docker-compose ps
docker-compose logs kafka
```

### Schema Issues

The Avro schema is in `src/main/avro/order.avsc`. After any changes, rebuild:

```bash
mvn clean package
```

## Next Steps

- Explore the code in `src/main/java/com/example/kafka/`
- Modify the Avro schema and see schema evolution
- Adjust retry logic in `application.yml`
- Add more products in `OrderProducer.java`
- Customize the dashboard UI
