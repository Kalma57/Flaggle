package com.example.flagdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class FlagdemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(FlagdemoApplication.class, args);
	}
}