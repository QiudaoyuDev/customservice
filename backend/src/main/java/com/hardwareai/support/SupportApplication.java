package com.hardwareai.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Application entry point for the single-instance knowledge operations service. */
@SpringBootApplication @EnableScheduling
public class SupportApplication {
  public static void main(String[] args) { SpringApplication.run(SupportApplication.class, args); }
}
