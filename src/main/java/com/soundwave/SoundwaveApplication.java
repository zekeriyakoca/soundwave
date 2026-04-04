package com.soundwave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.soundwave.infrastructure.persistence")
public class SoundwaveApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoundwaveApplication.class, args);
	}

}
