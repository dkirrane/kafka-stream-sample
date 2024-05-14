package com.github.dkirrane.kstream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
public class KStreamSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(KStreamSampleApplication.class, args);
	}

}
