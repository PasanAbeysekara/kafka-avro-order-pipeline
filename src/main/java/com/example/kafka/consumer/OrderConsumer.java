package com.example.kafka.consumer;

import com.example.kafka.avro.Order;
import com.example.kafka.service.OrderStatsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class OrderConsumer {

    private final OrderStatsService statsService;
    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final String ordersRetryTopic;
    private final String ordersDlqTopic;
    private final int maxRetryAttempts;
    private final Random random = new Random();

    public OrderConsumer(OrderStatsService statsService,
                        KafkaTemplate<String, Order> kafkaTemplate,
                        @Value("${kafka.topics.orders-retry}") String ordersRetryTopic,
                        @Value("${kafka.topics.orders-dlq}") String ordersDlqTopic,
                        @Value("${kafka.retry.max-attempts}") int maxRetryAttempts) {
        this.statsService = statsService;
        this.kafkaTemplate = kafkaTemplate;
        this.ordersRetryTopic = ordersRetryTopic;
        this.ordersDlqTopic = ordersDlqTopic;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    @KafkaListener(topics = "${kafka.topics.orders}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrder(ConsumerRecord<String, Order> record) {
        processOrder(record, 0);
    }

    @KafkaListener(topics = "${kafka.topics.orders-retry}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeRetryOrder(ConsumerRecord<String, Order> record) {
        int retryAttempt = getRetryAttempt(record);
        processOrder(record, retryAttempt);
    }

    private void processOrder(ConsumerRecord<String, Order> record, int retryAttempt) {
        Order order = record.value();
        log.info("Processing order: {} (attempt {})", order.getOrderId(), retryAttempt + 1);

        try {
            // Simulate random processing failures (10% chance)
            if (random.nextInt(10) == 0) {
                throw new RuntimeException("Simulated temporary failure");
            }

            // Process the order successfully
            statsService.recordOrder(order);
            log.info("Successfully processed order: {}", order.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process order: {} (attempt {})", order.getOrderId(), retryAttempt + 1, e);
            handleFailure(order, retryAttempt, e);
        }
    }

    private void handleFailure(Order order, int retryAttempt, Exception e) {
        if (retryAttempt < maxRetryAttempts) {
            // Send to retry topic with incremented attempt counter
            int nextAttempt = retryAttempt + 1;
            log.info("Sending order {} to retry topic (attempt {})", order.getOrderId(), nextAttempt);
            
            org.springframework.kafka.support.KafkaHeaders kafkaHeaders;
            org.apache.kafka.clients.producer.ProducerRecord<String, Order> producerRecord =
                new org.apache.kafka.clients.producer.ProducerRecord<>(
                    ordersRetryTopic,
                    order.getOrderId().toString(),
                    order
                );
            producerRecord.headers().add("retry-attempt", String.valueOf(nextAttempt).getBytes(StandardCharsets.UTF_8));
            
            kafkaTemplate.send(producerRecord)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            statsService.recordRetry();
                        }
                    });
        } else {
            // Send to DLQ after max retries
            log.error("Max retry attempts reached for order {}. Sending to DLQ", order.getOrderId());
            kafkaTemplate.send(ordersDlqTopic, order.getOrderId().toString(), order);
            statsService.recordDlq(order, "Max retries exceeded: " + e.getMessage());
        }
    }

    private int getRetryAttempt(ConsumerRecord<String, Order> record) {
        Header retryHeader = record.headers().lastHeader("retry-attempt");
        if (retryHeader != null) {
            return Integer.parseInt(new String(retryHeader.value(), StandardCharsets.UTF_8));
        }
        return 0;
    }

    @KafkaListener(topics = "${kafka.topics.orders-dlq}", groupId = "${spring.kafka.consumer.group-id}-dlq")
    public void consumeDlqOrder(ConsumerRecord<String, Order> record) {
        Order order = record.value();
        log.info("Order in DLQ: {}", order.getOrderId());
        // DLQ messages are just logged, already tracked in statsService
    }
}
