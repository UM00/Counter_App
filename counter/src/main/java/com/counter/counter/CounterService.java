package com.counter.counter;

import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CounterService {

    private static final String COUNTER_KEY ="counter";
    @Autowired
    private final CounterRepository counterRepository;
    private final Lock lock = new ReentrantLock();
    private final RedisTemplate<String, Integer> redisTemplate;
    private final KafkaTemplate<String ,Integer> kafkaTemplate;

    @Autowired
    public CounterService(CounterRepository counterRepository, RedisTemplate<String, Integer> redisTemplate, KafkaTemplate<String, Integer> kafkaTemplate) {
        this.counterRepository = counterRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }
    /*
    Simple Counter Increment
     */
    @Transactional
    public int add() {
        Counter counter = counterRepository.findById(1).orElseThrow(() -> new RuntimeException("Counter not found"));
        counter.setCount(counter.getCount() + 1);
        counterRepository.save(counter);
        return counter.getCount();
    }

    /*
    Simple Counter with Kafka
     */
    @Transactional("kafkaTransactionManager")
    public synchronized int addwithKafka() {
        Counter counter = counterRepository.findById(1).orElseThrow(() -> new RuntimeException("Counter not Found"));
        counter.setCount(counter.getCount() + 1);
        counterRepository.save(counter);

        kafkaTemplate.executeInTransaction(kafkaTemplate -> {
            kafkaTemplate.send("counter-updates", counter.getCount());
            return null;
        });

        return counter.getCount();
    }

    /*
Mutex-Lock Implemented
 */
    @Async
    @Transactional
    public CompletableFuture<Integer> addv1() {
        return CompletableFuture.supplyAsync(() -> {
            lock.lock();
            try {
                Counter counter = counterRepository.findById(1)
                        .orElseThrow(() -> new RuntimeException("Counter not found"));
                counter.setCount(counter.getCount() + 1);
                return counterRepository.save(counter).getCount();
            } catch (Exception e) {
                throw new RuntimeException("Error updating counter", e);
            } finally {
                lock.unlock();
            }
        });
    }

/*
Mutex-Lock Along with Redis
 */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Integer> addv2() {
        Integer currentCount = Math.toIntExact(redisTemplate.opsForValue().increment(COUNTER_KEY, 1));
        if (currentCount == 1) {
            Counter counter = counterRepository.findById(1)
                    .orElseThrow(() -> new RuntimeException("Counter not found"));
            currentCount = counter.getCount()+1;
            redisTemplate.opsForValue().set(COUNTER_KEY, currentCount);
        }
        /**
         * Update the database Async-ly
         */
        Integer finalCurrentCount = currentCount;
        return CompletableFuture.supplyAsync(() -> {
            Counter counter = counterRepository.findById(1)
                    .orElseThrow(() -> new RuntimeException("Counter not found"));
            counter.setCount(finalCurrentCount);
            counterRepository.save(counter);
            return finalCurrentCount;
        });
    }

    /**
     * Increment With Lock on Database
     */
    @Transactional
    public Integer withLock(){
        Counter counter=counterRepository.findByIdForUpdate(1).orElseThrow(()->new RuntimeException("Counter Not Found"));
        counter.setCount(counter.getCount()+1);
        counterRepository.save(counter);
        return counter.getCount();
    }
/*
Database Lock Along With Redis
 */
    @Transactional
    public Integer LockWithRedis() {
        Integer currentCount = Math.toIntExact(redisTemplate.opsForValue().increment(COUNTER_KEY, 1));
        if (currentCount == 1) {
            Counter counter = counterRepository.findByIdForUpdate(1).orElseThrow(() -> new RuntimeException("Counter Not Found"));
            counter.setCount(counter.getCount() + 1);
            redisTemplate.opsForValue().set(COUNTER_KEY, currentCount);
        }
        Integer finalCurrentCount = currentCount;
        Counter counter = counterRepository.findByIdForUpdate(1).orElseThrow(() -> new RuntimeException("Counter not Found"));
        counter.setCount(counter.getCount() + 1);
        counterRepository.save(counter);
        return finalCurrentCount;
    }

/*
Database Lock Along With Redis With Implementation of Kafka
 */
@Transactional
public Integer LockWithRedisAndKafka() {
    Integer currentCount = Math.toIntExact(redisTemplate.opsForValue().increment(COUNTER_KEY, 1));
    if (currentCount == 1) {
        Counter counter = counterRepository.findByIdForUpdate(1).orElseThrow(() -> new RuntimeException("Counter Not Found"));
        counter.setCount(counter.getCount() + 1);
        redisTemplate.opsForValue().set(COUNTER_KEY, currentCount);
    }
    Integer finalCurrentCount = currentCount;
    Counter counter = counterRepository.findByIdForUpdate(1).orElseThrow(() -> new RuntimeException("Counter not Found"));
    counter.setCount(counter.getCount() + 1);
    counterRepository.save(counter);

    kafkaTemplate.send("counter-updates",finalCurrentCount);

    return finalCurrentCount;
}
}
