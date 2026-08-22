package com.saumik.photovault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PhotovaultApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhotovaultApplication.class, args);
	}

}
