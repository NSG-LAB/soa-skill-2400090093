package com.example.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    @KafkaListener(
        topics = "api-communication",
        groupId = "consumer-group"
    )
    public void receiveMessage(String message) {

        System.out.println("=================================");
        System.out.println("Message received from Kafka:");
        System.out.println(message);
        System.out.println("=================================");
    }
}
