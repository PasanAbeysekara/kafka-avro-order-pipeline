package com.example.kafka.service;

import com.example.kafka.avro.Order;
import com.example.kafka.model.OrderStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderStatsServiceTest {

    private OrderStatsService statsService;
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        statsService = new OrderStatsService(messagingTemplate);
    }

    @Test
    void testRecordOrder() {
        Order order = Order.newBuilder()
                .setOrderId("test-123")
                .setProduct("Laptop")
                .setPrice(500.0f)
                .build();

        statsService.recordOrder(order);

        assertEquals(1, statsService.getTotalOrders().get());
        assertEquals(500.0, statsService.getRunningAverage(), 0.01);
    }

    @Test
    void testRunningAverage() {
        Order order1 = Order.newBuilder()
                .setOrderId("test-1")
                .setProduct("Laptop")
                .setPrice(100.0f)
                .build();

        Order order2 = Order.newBuilder()
                .setOrderId("test-2")
                .setProduct("Mouse")
                .setPrice(200.0f)
                .build();

        statsService.recordOrder(order1);
        statsService.recordOrder(order2);

        assertEquals(2, statsService.getTotalOrders().get());
        assertEquals(150.0, statsService.getRunningAverage(), 0.01);
    }

    @Test
    void testRecordRetry() {
        statsService.recordRetry();
        assertEquals(1, statsService.getRetryCount().get());
    }

    @Test
    void testRecordDlq() {
        Order order = Order.newBuilder()
                .setOrderId("test-dlq")
                .setProduct("Tablet")
                .setPrice(300.0f)
                .build();

        statsService.recordDlq(order, "Test failure");

        assertEquals(1, statsService.getDlqCount().get());
        assertEquals(1, statsService.getDlqMessages().size());
    }

    @Test
    void testGetStats() {
        Order order = Order.newBuilder()
                .setOrderId("test-stats")
                .setProduct("Phone")
                .setPrice(400.0f)
                .build();

        statsService.recordOrder(order);
        statsService.recordRetry();
        statsService.recordDlq(order, "Test");

        OrderStats stats = statsService.getStats();

        assertEquals(1, stats.getTotalOrders());
        assertEquals(400.0, stats.getRunningAverage(), 0.01);
        assertEquals(1, stats.getRetryCount());
        assertEquals(1, stats.getDlqCount());
    }
}
