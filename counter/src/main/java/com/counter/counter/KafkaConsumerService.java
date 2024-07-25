package com.counter.counter;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

@Service
public class KafkaConsumerService {

    @Autowired
    private Executor taskExecutor;

    // Initialize seenIds as a CopyOnWriteArrayList for thread safety

    @KafkaListener(topics = "counter-updates", groupId = "counter-group")
    public void listen(Integer count) {
        processMessage(count);
    }

    private final List<Integer> seenIds = new CopyOnWriteArrayList<>();

    @Async("taskExecutor")
    public void processMessage(Integer count) {
        if (!seenIds.contains(count)) {
            System.out.println("Processing message: " + count);
            seenIds.add(count); // Mark this ID as seen

        }
    }



}
