package com.example.kafka.controller;

import com.example.kafka.avro.Order;
import com.example.kafka.model.OrderDTO;
import com.example.kafka.model.OrderStats;
import com.example.kafka.producer.OrderProducer;
import com.example.kafka.service.OrderStatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Slf4j
public class OrderController {

    private final OrderProducer orderProducer;
    private final OrderStatsService statsService;

    public OrderController(OrderProducer orderProducer, OrderStatsService statsService) {
        this.orderProducer = orderProducer;
        this.statsService = statsService;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderDTO> createOrder() {
        Order order = orderProducer.produceOrder();
        log.info("Created order via API: {}", order.getOrderId());
        
        OrderDTO orderDTO = new OrderDTO(
                order.getOrderId().toString(),
                order.getProduct().toString(),
                order.getPrice(),
                "SENT",
                System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(orderDTO);
    }

    @GetMapping("/orders/recent")
    public ResponseEntity<List<OrderDTO>> getRecentOrders() {
        return ResponseEntity.ok(statsService.getRecentOrders());
    }

    @GetMapping("/orders/dlq")
    public ResponseEntity<List<OrderDTO>> getDlqMessages() {
        return ResponseEntity.ok(statsService.getDlqMessages());
    }

    @GetMapping("/stats")
    public ResponseEntity<OrderStats> getStats() {
        return ResponseEntity.ok(statsService.getStats());
    }
}
