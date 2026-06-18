package com.samgiabao.smartInventory;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.TimeZone;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.samgiabao.smartInventory",
		"controller",
		"controller.api",
		"service",
		"config"
})
@EnableJpaRepositories(basePackages = {
		"repository"
})
@EntityScan(basePackages = {
		"entity"
})
public class SmartInventoryApplication {

	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
	}

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		SpringApplication.run(SmartInventoryApplication.class, args);
	}
}