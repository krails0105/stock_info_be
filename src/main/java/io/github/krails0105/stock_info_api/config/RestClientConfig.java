package io.github.krails0105.stock_info_api.config;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/*
 * ============================================================================
 * RestClient 설정 클래스
 * ============================================================================
 */
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

  private final KisRestClientProperties kisProperties;

  @Bean
  RestClient kisRestClient(RestClient.Builder builder) {
    String baseUrl =
        Objects.requireNonNull(kisProperties.getBaseUrl(), "rest-client.kis.base-url 설정이 필요합니다");

    return builder
        .baseUrl(baseUrl)
        .defaultHeader("content-type", "application/json; charset=utf-8")
        .build();
  }
}
