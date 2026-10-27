package com.thoonidev.InventoryMgtSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InventoryMgtSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryMgtSystemApplication.class, args);
	}

}
