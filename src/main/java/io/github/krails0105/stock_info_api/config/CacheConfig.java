package io.github.krails0105.stock_info_api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 설정.
 *
 * <p>Caffeine 인메모리 캐시를 사용하여 외부 API 호출 결과를 캐싱한다. 차트 데이터의 경우 60초 TTL로 설정하여 장중 변동을 적절히 반영한다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /** 차트 캐시 이름 */
  public static final String CHART_CACHE = "chartCache";

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager(CHART_CACHE);
    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS) // TTL 60초
            .maximumSize(500) // 최대 500개 엔트리 (종목×range 조합)
            .recordStats());
    return cacheManager;
  }
}
