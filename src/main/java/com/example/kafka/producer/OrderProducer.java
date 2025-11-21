package com.example.kafka.producer;

import com.example.kafka.avro.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OrderProducer {

    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final String ordersTopic;
    private final Random random = new Random();
    private final String[] products = {
            "Laptop", "Smartphone", "Headphones", "Keyboard", "Mouse",
            "Monitor", "Tablet", "Camera", "Smartwatch", "Speaker"
    };

    public OrderProducer(KafkaTemplate<String, Order> kafkaTemplate,
                        @Value("${kafka.topics.orders}") String ordersTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.ordersTopic = ordersTopic;
    }

    public Order produceOrder() {
        String orderId = UUID.randomUUID().toString();
        String product = products[random.nextInt(products.length)];
        float price = 10.0f + (random.nextFloat() * 990.0f); // Random price between 10 and 1000

        Order order = Order.newBuilder()
                .setOrderId(orderId)
                .setProduct(product)
                .setPrice(price)
                .build();

        sendOrder(order);
        return order;
    }

    public void sendOrder(Order order) {
        CompletableFuture<SendResult<String, Order>> future = 
            kafkaTemplate.send(ordersTopic, order.getOrderId().toString(), order);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent order: {} to topic: {} with offset: {}",
                        order.getOrderId(), ordersTopic, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send order: {}", order.getOrderId(), ex);
            }
        });
    }
}
