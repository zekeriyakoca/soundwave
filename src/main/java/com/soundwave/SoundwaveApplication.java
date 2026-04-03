package com.soundwave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SoundwaveApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoundwaveApplication.class, args);
	}

}
