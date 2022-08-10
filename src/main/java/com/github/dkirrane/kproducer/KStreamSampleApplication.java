package com.github.dkirrane.kproducer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
public class KStreamSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(KStreamSampleApplication.class, args);
	}

}
