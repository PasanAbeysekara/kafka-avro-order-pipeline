package com.example.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStats {
    private long totalOrders;
    private double runningAverage;
    private int retryCount;
    private int dlqCount;
}
