package com.example.producer;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProducerController {

    private final KafkaProducer kafkaProducer;

    public ProducerController(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("/send")
    public String sendMessage(@RequestParam String message) {

        kafkaProducer.sendMessage(message);

        return "Message sent to Kafka: " + message;
    }
}
