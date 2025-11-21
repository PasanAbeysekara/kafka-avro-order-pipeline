package com.example.kafka.service;

import com.example.kafka.avro.Order;
import com.example.kafka.model.OrderDTO;
import com.example.kafka.model.OrderStats;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class OrderStatsService {

    private final SimpMessagingTemplate messagingTemplate;
    
    @Getter
    private final AtomicLong totalOrders = new AtomicLong(0);
    private final AtomicLong priceSum = new AtomicLong(0);
    
    @Getter
    private final AtomicInteger retryCount = new AtomicInteger(0);
    
    @Getter
    private final AtomicInteger dlqCount = new AtomicInteger(0);
    
    @Getter
    private final ConcurrentLinkedQueue<OrderDTO> recentOrders = new ConcurrentLinkedQueue<>();
    
    @Getter
    private final ConcurrentLinkedQueue<OrderDTO> dlqMessages = new ConcurrentLinkedQueue<>();
    
    private static final int MAX_RECENT_ORDERS = 50;
    private static final int MAX_DLQ_MESSAGES = 50;

    public OrderStatsService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void recordOrder(Order order) {
        totalOrders.incrementAndGet();
        
        // Convert float to long for atomic operation (multiply by 100 to preserve 2 decimal places)
        long priceAsLong = (long) (order.getPrice() * 100);
        priceSum.addAndGet(priceAsLong);

        OrderDTO orderDTO = new OrderDTO(
                order.getOrderId().toString(),
                order.getProduct().toString(),
                order.getPrice(),
                "PROCESSED",
                System.currentTimeMillis()
        );

        addToRecentOrders(orderDTO);
        
        // Send update via WebSocket
        messagingTemplate.convertAndSend("/topic/orders", orderDTO);
        messagingTemplate.convertAndSend("/topic/stats", getStats());
    }

    public void recordRetry() {
        retryCount.incrementAndGet();
        messagingTemplate.convertAndSend("/topic/stats", getStats());
    }

    public void recordDlq(Order order, String reason) {
        dlqCount.incrementAndGet();

        OrderDTO orderDTO = new OrderDTO(
                order.getOrderId().toString(),
                order.getProduct().toString(),
                order.getPrice(),
                "DLQ: " + reason,
                System.currentTimeMillis()
        );

        addToDlqMessages(orderDTO);
        
        // Send update via WebSocket
        messagingTemplate.convertAndSend("/topic/dlq", orderDTO);
        messagingTemplate.convertAndSend("/topic/stats", getStats());
    }

    private void addToRecentOrders(OrderDTO order) {
        recentOrders.offer(order);
        if (recentOrders.size() > MAX_RECENT_ORDERS) {
            recentOrders.poll();
        }
    }

    private void addToDlqMessages(OrderDTO order) {
        dlqMessages.offer(order);
        if (dlqMessages.size() > MAX_DLQ_MESSAGES) {
            dlqMessages.poll();
        }
    }

    public double getRunningAverage() {
        long total = totalOrders.get();
        if (total == 0) {
            return 0.0;
        }
        // Divide by 100 to convert back from the scaled integer
        return (priceSum.get() / 100.0) / total;
    }

    public OrderStats getStats() {
        return new OrderStats(
                totalOrders.get(),
                getRunningAverage(),
                retryCount.get(),
                dlqCount.get()
        );
    }

    public List<OrderDTO> getRecentOrders() {
        return new ArrayList<>(recentOrders);
    }

    public List<OrderDTO> getDlqMessages() {
        return new ArrayList<>(dlqMessages);
    }
}
