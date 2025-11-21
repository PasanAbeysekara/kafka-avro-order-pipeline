package com.example.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092",
    "kafka.topics.orders=test-orders",
    "kafka.topics.orders-retry=test-orders-retry",
    "kafka.topics.orders-dlq=test-orders-dlq"
})
class KafkaAvroOrderPipelineApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
    }
}
