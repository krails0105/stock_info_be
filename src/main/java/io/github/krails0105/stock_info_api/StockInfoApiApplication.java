package io.github.krails0105.stock_info_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockInfoApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(StockInfoApiApplication.class, args);
  }
}
