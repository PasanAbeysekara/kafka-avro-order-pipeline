# Kafka Avro Order Pipeline

A production-ready Kafka-based order processing pipeline built with Spring Boot, demonstrating real-time event streaming, Avro serialization, retry logic, and dead letter queue (DLQ) handling.

## 🚀 Features

- **Kafka Producer/Consumer**: Real-time order processing with Apache Kafka
- **Avro Schema**: Type-safe message serialization with schema evolution support
- **Real-time Analytics**: Running average calculation of order prices
- **Retry Logic**: Automatic retry mechanism for temporary failures
- **Dead Letter Queue (DLQ)**: Separate queue for permanently failed messages
- **REST API**: Create orders and query statistics via REST endpoints
- **WebSocket Support**: Live updates pushed to dashboard in real-time
- **Interactive Dashboard**: Beautiful HTML/JavaScript UI showing:
  - Live order stream
  - Real-time average price chart
  - Retry and DLQ metrics
  - Failed message tracking

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (for running Kafka)

## 🏗️ Project Structure

```
kafka-avro-order-pipeline/
├── src/
│   ├── main/
│   │   ├── avro/
│   │   │   └── order.avsc              # Avro schema definition
│   │   ├── java/com/example/kafka/
│   │   │   ├── KafkaAvroOrderPipelineApplication.java
│   │   │   ├── config/
│   │   │   │   ├── KafkaConfig.java    # Kafka topics configuration
│   │   │   │   └── WebSocketConfig.java # WebSocket configuration
│   │   │   ├── consumer/
│   │   │   │   └── OrderConsumer.java  # Kafka consumer with retry/DLQ
│   │   │   ├── producer/
│   │   │   │   └── OrderProducer.java  # Kafka producer
│   │   │   ├── service/
│   │   │   │   └── OrderStatsService.java # Statistics tracking
│   │   │   ├── controller/
│   │   │   │   └── OrderController.java # REST API endpoints
│   │   │   └── model/
│   │   │       ├── OrderDTO.java
│   │   │       └── OrderStats.java
│   │   └── resources/
│   │       ├── application.yml          # Application configuration
│   │       └── static/
│   │           └── index.html          # Dashboard UI
│   └── test/                           # Test files
├── pom.xml                             # Maven dependencies
└── README.md
```

## 🛠️ Setup Instructions

### Step 1: Start Kafka Infrastructure

Create a `docker-compose.yml` file:

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
```

Start the services:

```bash
docker-compose up -d
```

Wait for all services to be healthy (~30 seconds):

```bash
docker-compose ps
```

### Step 2: Build the Application

```bash
mvn clean package
```

This will:
- Generate Java classes from the Avro schema
- Compile the application
- Run tests
- Create an executable JAR

### Step 3: Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/kafka-avro-order-pipeline-1.0.0.jar
```

The application will start on port 8080.

## 🎮 How to Use

### Access the Dashboard

Open your browser and navigate to:
```
http://localhost:8080
```

You'll see the interactive dashboard showing:
- **Total Orders**: Count of successfully processed orders
- **Average Price**: Real-time running average of all order prices
- **Retry Count**: Number of retry attempts
- **DLQ Messages**: Number of permanently failed messages
- **Average Price Chart**: Line chart showing price trends
- **Recent Orders**: Live stream of processed orders
- **Dead Letter Queue**: Failed messages with error reasons

### Create Orders via Dashboard

Click the "Create New Order" button to generate a random order. The order will:
1. Be sent to the Kafka `orders` topic
2. Be consumed and processed (with 10% simulated failure rate)
3. Appear in the "Recent Orders" section if successful
4. Be retried up to 3 times if it fails temporarily
5. End up in DLQ if all retries are exhausted

### REST API Endpoints

#### Create a New Order
```bash
curl -X POST http://localhost:8080/api/orders
```

Response:
```json
{
  "orderId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "product": "Laptop",
  "price": 599.99,
  "status": "SENT",
  "timestamp": 1699999999999
}
```

#### Get Statistics
```bash
curl http://localhost:8080/api/stats
```

Response:
```json
{
  "totalOrders": 42,
  "runningAverage": 523.45,
  "retryCount": 5,
  "dlqCount": 2
}
```

#### Get Recent Orders
```bash
curl http://localhost:8080/api/orders/recent
```

#### Get DLQ Messages
```bash
curl http://localhost:8080/api/orders/dlq
```

## 🔧 Configuration

Key configuration in `application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092  # Kafka broker address

kafka:
  topics:
    orders: orders              # Main orders topic
    orders-retry: orders-retry  # Retry topic
    orders-dlq: orders-dlq      # Dead letter queue topic
  retry:
    max-attempts: 3             # Maximum retry attempts
    backoff-ms: 5000            # Backoff between retries
```

## 🧪 Testing

Generate some test load by creating multiple orders:

```bash
# Create 10 orders
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders
  sleep 1
done
```

Watch the dashboard update in real-time as orders are:
- Created and sent to Kafka
- Consumed and processed
- Retried on failures
- Moved to DLQ after max retries

## 📊 Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   REST API  │─────▶│    Kafka     │─────▶│  Consumer   │
│  Dashboard  │      │   Producer   │      │  + Retry    │
└─────────────┘      └──────────────┘      └─────────────┘
       │                    │                      │
       │                    ▼                      ▼
       │            ┌──────────────┐       ┌─────────────┐
       │            │ orders topic │       │ Statistics  │
       │            └──────────────┘       │   Service   │
       │                    │              └─────────────┘
       │                    │                      │
       ▼                    ▼                      ▼
┌─────────────┐    ┌──────────────┐      ┌─────────────┐
│  WebSocket  │◀───│   Retry/DLQ  │      │  WebSocket  │
│   Updates   │    │    Topics    │      │  Broadcast  │
└─────────────┘    └──────────────┘      └─────────────┘
```

## 🔍 Key Components

### Avro Schema (`order.avsc`)
Defines the structure of order messages with type safety and schema evolution support.

### OrderProducer
Generates random orders with products from a predefined list and random prices between $10-$1000.

### OrderConsumer
- Consumes orders from the main topic
- Simulates 10% failure rate for demonstration
- Implements retry logic with exponential backoff
- Sends permanently failed messages to DLQ

### OrderStatsService
- Tracks real-time statistics
- Calculates running average using atomic operations
- Broadcasts updates via WebSocket

### Dashboard
- Real-time visualization using Chart.js
- WebSocket integration via STOMP protocol
- Responsive design with smooth animations

## 🐛 Troubleshooting

### Kafka Connection Issues
Ensure Kafka and Schema Registry are running:
```bash
docker-compose ps
```

Check logs:
```bash
docker-compose logs kafka
docker-compose logs schema-registry
```

### Port Already in Use
If port 8080 is in use, change it in `application.yml`:
```yaml
server:
  port: 8090
```

### Avro Serialization Issues
The application uses custom Avro serializers (no Schema Registry required). If you encounter serialization issues, check that the Avro schema was generated correctly:
```bash
ls target/generated-sources/avro/com/example/kafka/avro/
```

## 📦 Technologies Used

- **Spring Boot 3.1.5**: Application framework
- **Apache Kafka**: Message broker
- **Apache Avro 1.11.3**: Schema serialization (with custom serializers)
- **WebSocket (STOMP)**: Real-time updates
- **Chart.js**: Data visualization
- **Maven**: Build tool

## 🚦 Next Steps

- Add authentication and authorization
- Implement monitoring with Prometheus/Grafana
- Add circuit breaker pattern
- Configure multiple consumer instances
- Add message ordering guarantees
- Implement exactly-once semantics
- Add schema evolution examples
- Deploy to Kubernetes

## 📝 License

This project is open source and available for educational purposes.
