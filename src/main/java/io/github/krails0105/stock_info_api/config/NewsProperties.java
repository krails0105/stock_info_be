package io.github.krails0105.stock_info_api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 뉴스 수집/처리 관련 설정.
 *
 * <p>application.yml의 `news.*` 프로퍼티를 바인딩한다.
 */
@Configuration
@ConfigurationProperties(prefix = "news")
@Getter
@Setter
public class NewsProperties {

  private Collection collection = new Collection();
  private Processing processing = new Processing();
  private Freshness freshness = new Freshness();
  private Clustering clustering = new Clustering();

  /** 수집 관련 설정. */
  @Getter
  @Setter
  public static class Collection {
    /** 수집 활성화 여부. */
    private boolean enabled = true;

    /** 수집 주기 (분). */
    private int intervalMinutes = 15;
  }

  /** 처리 관련 설정. */
  @Getter
  @Setter
  public static class Processing {
    /** 배치 처리 크기. */
    private int batchSize = 100;

    /** 처리 주기 (분). */
    private int intervalMinutes = 5;
  }

  /** 뉴스 신선도 설정. */
  @Getter
  @Setter
  public static class Freshness {
    /** HIGH 중요도 기준 시간. */
    private int highHours = 24;

    /** MEDIUM 중요도 기준 시간. */
    private int mediumHours = 72;
  }

  /** 클러스터링 설정. */
  @Getter
  @Setter
  public static class Clustering {
    /** 유사도 임계값 (0.0~1.0). */
    private double similarityThreshold = 0.6;

    /** 클러스터링 윈도우 (시간). */
    private int windowHours = 72;
  }
}
