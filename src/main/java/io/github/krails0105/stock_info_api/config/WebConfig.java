package io.github.krails0105.stock_info_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        // "/api/*"는 "/api/무언가" 한 단계(예: /api/indices)까지만 매칭되고,
        // "/api/**"는 "/api" 하위의 모든 경로(여러 단계, 예: /api/sector/list, /api/stocks/123 등)까지 모두 매칭합니다.
        // 즉, "/api/*"는 한 단계만, "/api/**"는 모든 하위 경로에 적용됩니다.
        .addMapping("/api/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
