package com.counter.counter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class CounterController {

    private static final String COUNTER_KEY = "counter_key";
    private final CounterService counterService;
    @Autowired
    private CounterRepository counterRepository;
    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    @Autowired
    public CounterController(CounterService counterService) {
        this.counterService = counterService;
    }
    /*
    Simple Endpoint to Increment Counter
     */
    @PostMapping("/add")
    public ResponseEntity<Integer> add() {
        int count = counterService.add();
        return ResponseEntity.status(HttpStatus.OK).body(count);
    }
/*
Endpoint with Mutex-Lock Implemented
 */
    @PostMapping("/addv1")
    public CompletableFuture<ResponseEntity<Integer>> addv1() {
        return counterService.addv1()
                .thenApply(count -> ResponseEntity.ok(count))
                .exceptionally(ex -> ResponseEntity.status(500).body(null));
    }
/*
Mutex-Lock Implemented-Along with Redis
 */
    @PostMapping("/addv2")
    public CompletableFuture<ResponseEntity<Integer>> addv2() {
        return counterService.addv1()
                .thenApply(count -> ResponseEntity.status(HttpStatus.OK).body(count));
    }
/*
Enpoint to Test Redis Availabilty
 */
    @GetMapping("/ping")
    public String ping() {
        try {
            redisTemplate.opsForValue().set("testKey", 123);
            Integer value = redisTemplate.opsForValue().get("testKey");
            return "Redis is connected. Test key value: " + value;
        } catch (Exception e) {
            return "Error connecting to Redis: " + e.getMessage();
        }
    }
/*
Enpoint To get Results from the Redis
 */
    @GetMapping("/withredis")
    public Integer getCountWithRedis(){
        try {
            Integer currentCount = redisTemplate.opsForValue().get(COUNTER_KEY);
            if (currentCount == null){
                Counter counter = counterRepository.findById(1)
                        .orElseThrow(() -> new RuntimeException("Counter not found"));
                currentCount = counter.getCount();
                redisTemplate.opsForValue().set(COUNTER_KEY, currentCount);
            }
            return currentCount;
        } catch (Exception e) {
            System.err.println("Error:It not working bro " + e.getMessage());
            throw e;
        }
    }
/*
Endpoint to get Results without Redis
 */
    @GetMapping("/without-redis")
    public Integer getCountWithoutRedis(){
        Counter counter=counterRepository.findById(1).orElseThrow(()->new RuntimeException("Counter Not Found"));
        return counter.getCount();
    }
/*
Database Lock Mechanisim Implemented
 */
    @PostMapping("/withLock")
    public Integer getCountWithLock(){
        return counterService.withLock();
    }
/*
Endpoint With DB-Lock Along with Redis
 */
    @GetMapping("/GLockwithRedis")
    public Integer getCountWithLockWithRedis(){
        try {
            Integer currentcount = redisTemplate.opsForValue().get(COUNTER_KEY);
            if (currentcount == 1) {
                Counter counter = counterRepository.findByIdForUpdate(1).orElseThrow(() -> new RuntimeException("Counter Not Found"));
                currentcount = counter.getCount();
                redisTemplate.opsForValue().set(COUNTER_KEY, currentcount);
            }
            return currentcount;
        }catch (Exception e){
            throw e;
        }
    }
    /*
   Endpoint With DB-Lock Along with Redis
    */
    @PostMapping("/LockWithRedis")
    public Integer getCountewithLockwithRedis(){
        return counterService.LockWithRedis();
    }

  /*
  Endpoint With DB-Lock with Redis and Kafka
    No need As Kafka Handles all So Lock and Redis are of no use
   */
    @PostMapping("/LockWithRedisAndKafka")
    public Integer getCounterWithLockRedisandKafka(){
        return counterService.LockWithRedisAndKafka();
    }

}
