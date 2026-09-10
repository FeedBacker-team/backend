package com.feedbacker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FeedbackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(FeedbackerApplication.class, args);
	}

}
