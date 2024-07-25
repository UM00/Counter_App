package com.counter.counter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableAsync
public class CounterApplication {

	private final CounterService counterService;
	private final Executor taskExecutor;

	public CounterApplication(CounterService counterService, Executor taskExecutor) {
		this.counterService = counterService;
		this.taskExecutor = taskExecutor;
	}

	public static void main(String[] args) {
		SpringApplication.run(CounterApplication.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return args -> executeTasks();
	}

	private void executeTasks() {
		long startTime = System.nanoTime();
		CompletableFuture<?>[] futures = new CompletableFuture[1000];
		for (int i = 0; i < 1000; i++) {
			futures[i] = CompletableFuture.runAsync(() -> {
				try {
					counterService.addwithKafka();
				} catch (Exception e) {
					System.err.println("Exception during execution: " + e.getMessage());
				}
			}, taskExecutor);
		}
		CompletableFuture.allOf(futures).join();

		long endTime = System.nanoTime();
		long timeTaken = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
		System.out.println("Time taken for 1000 executions: " + timeTaken + " ms");

		if (taskExecutor instanceof ThreadPoolTaskExecutor) {
			((ThreadPoolTaskExecutor) taskExecutor).shutdown();
		}
	}
}
