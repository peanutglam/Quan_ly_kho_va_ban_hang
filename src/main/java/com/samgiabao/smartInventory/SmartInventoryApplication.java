package com.samgiabao.smartInventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.samgiabao.smartInventory", "controller", "service", "config"})
@EntityScan(basePackages = "entity")
@EnableJpaRepositories(basePackages = "repository")
public class SmartInventoryApplication {
	public static void main(String[] args) {
		SpringApplication.run(SmartInventoryApplication.class, args);
	}
}
