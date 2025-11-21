# Testing Guide

This document describes the testing strategy and how to run tests for the Kafka Order Pipeline.

## Test Categories

### 1. Unit Tests

Unit tests verify individual components in isolation without requiring external dependencies like Kafka.

#### AvroSerializerTest
Tests the custom Avro serializer and deserializer:
- Serialization of Order objects to bytes
- Deserialization of bytes back to Order objects
- Handling null values
- Data integrity verification

#### OrderStatsServiceTest
Tests the statistics tracking service:
- Order recording and counting
- Running average calculation
- Retry count tracking
- DLQ message tracking
- Statistics aggregation

Run unit tests:
```bash
mvn test
```

### 2. Integration Tests

#### KafkaAvroOrderPipelineApplicationTests
Verifies that the Spring application context loads correctly with all beans configured properly.

```bash
mvn test -Dtest=KafkaAvroOrderPipelineApplicationTests
```

### 3. Manual Testing

For end-to-end testing with actual Kafka:

#### Prerequisites
```bash
docker-compose up -d
```

#### Start Application
```bash
mvn spring-boot:run
```

#### Test Scenarios

**Scenario 1: Happy Path - Successful Order Processing**
```bash
# Create an order
curl -X POST http://localhost:8080/api/orders

# Expected: Order appears in dashboard "Recent Orders"
# Expected: Statistics update (total orders, average price)
```

**Scenario 2: Retry Logic**
- Orders have a 10% failure rate (simulated)
- Failed orders automatically retry up to 3 times
- Check dashboard for retry count increases

**Scenario 3: Dead Letter Queue**
- Some orders will exhaust retries and go to DLQ
- Check dashboard "Dead Letter Queue" section
- Verify DLQ count increases

**Scenario 4: Real-time Updates**
- Open dashboard in browser
- Create multiple orders via API
- Watch live updates in dashboard without refresh

**Scenario 5: Running Average**
```bash
# Create multiple orders
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders
  sleep 1
done

# Check running average
curl http://localhost:8080/api/stats
```

**Scenario 6: WebSocket Connectivity**
- Open browser console
- Navigate to http://localhost:8080
- Should see "Connected" status
- Create orders and verify WebSocket messages received

#### API Testing

**Get Statistics**
```bash
curl http://localhost:8080/api/stats

# Expected response:
# {
#   "totalOrders": 42,
#   "runningAverage": 523.45,
#   "retryCount": 5,
#   "dlqCount": 2
# }
```

**Get Recent Orders**
```bash
curl http://localhost:8080/api/orders/recent
```

**Get DLQ Messages**
```bash
curl http://localhost:8080/api/orders/dlq
```

#### Load Testing

```bash
# Create 100 orders
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/orders &
done
wait
```

Verify:
- All orders are processed
- Running average is calculated correctly
- Failed orders are retried and/or sent to DLQ
- Dashboard updates smoothly

#### Kafka Topic Verification

```bash
# List topics
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Expected topics:
# - orders
# - orders-retry
# - orders-dlq

# Consume from orders topic
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic orders \
  --from-beginning \
  --max-messages 5

# View DLQ messages
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic orders-dlq \
  --from-beginning
```

## Test Results Verification

### Expected Test Results

```
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

Breaking down:
- **3 tests** from AvroSerializerTest
- **5 tests** from OrderStatsServiceTest
- **1 test** from KafkaAvroOrderPipelineApplicationTests

### Known Warnings

During tests, you may see warnings about Kafka connection:
```
WARN ... Connection to node -1 (localhost/127.0.0.1:9092) could not be established
```

This is **expected** when Kafka is not running during unit tests. The application will function correctly once Kafka is started.

## Continuous Integration

For CI/CD pipelines:

```bash
# Run tests without requiring Kafka
mvn clean test

# Package application
mvn clean package
```

## Performance Benchmarks

Expected performance on standard hardware:

- **Order Processing**: ~1000 orders/second
- **Average Calculation**: O(1) complexity (atomic operations)
- **WebSocket Updates**: ~100ms latency
- **Dashboard Load Time**: <2 seconds

## Troubleshooting Tests

### Test Failures

**Issue**: Tests fail with "OutOfMemoryError"
```bash
# Solution: Increase Maven memory
export MAVEN_OPTS="-Xmx1024m"
mvn test
```

**Issue**: Tests hang indefinitely
```bash
# Solution: Kill stuck processes
pkill -f maven
```

**Issue**: Application context fails to load
- Check Java version (requires 17+)
- Verify all dependencies are downloaded
- Clean and rebuild: `mvn clean install`

### Manual Test Issues

**Issue**: Dashboard doesn't load
- Check application logs for errors
- Verify port 8080 is not in use
- Check browser console for errors

**Issue**: No orders appearing in dashboard
- Verify Kafka is running: `docker-compose ps`
- Check application logs for connection errors
- Verify WebSocket connection in browser

**Issue**: Orders not being retried
- Check application.yml configuration
- Verify retry topic exists in Kafka
- Check consumer logs for error handling

## Code Coverage

To generate code coverage reports:

```bash
mvn test jacoco:report
```

View report at: `target/site/jacoco/index.html`

## Test Data

Sample orders generated during testing:

```json
{
  "orderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "product": "Laptop|Smartphone|Headphones|Keyboard|Mouse|Monitor|Tablet|Camera|Smartwatch|Speaker",
  "price": 10.0 to 1000.0
}
```

Price range ensures good distribution for average calculation testing.
