package com.example.kafka.serializer;

import com.example.kafka.avro.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvroSerializerTest {

    @Test
    void testSerializeDeserialize() {
        // Create test order
        Order originalOrder = Order.newBuilder()
                .setOrderId("test-123")
                .setProduct("TestProduct")
                .setPrice(99.99f)
                .build();

        // Serialize
        AvroSerializer<Order> serializer = new AvroSerializer<>();
        byte[] serialized = serializer.serialize("test-topic", originalOrder);

        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        // Deserialize
        AvroDeserializer<Order> deserializer = new AvroDeserializer<>(Order.class);
        Order deserializedOrder = deserializer.deserialize("test-topic", serialized);

        // Verify
        assertNotNull(deserializedOrder);
        assertEquals(originalOrder.getOrderId().toString(), deserializedOrder.getOrderId().toString());
        assertEquals(originalOrder.getProduct().toString(), deserializedOrder.getProduct().toString());
        assertEquals(originalOrder.getPrice(), deserializedOrder.getPrice(), 0.01f);
    }

    @Test
    void testSerializeNull() {
        AvroSerializer<Order> serializer = new AvroSerializer<>();
        byte[] result = serializer.serialize("test-topic", null);
        assertNull(result);
    }

    @Test
    void testDeserializeNull() {
        AvroDeserializer<Order> deserializer = new AvroDeserializer<>(Order.class);
        Order result = deserializer.deserialize("test-topic", null);
        assertNull(result);
    }
}
