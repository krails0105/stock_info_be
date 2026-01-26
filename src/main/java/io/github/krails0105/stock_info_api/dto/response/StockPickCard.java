package io.github.krails0105.stock_info_api.dto.response;

import io.github.krails0105.stock_info_api.dto.ScoreLabel;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 홈 Watchlist Picks 개별 카드 DTO */
@Getter
@Builder
public class StockPickCard {
  private String code;
  private String name;
  private String sectorName;
  private int scoreValue;
  private ScoreLabel grade;
  private PickBucket pickBucket;
  private List<String> reasons;
  private String caution;
  private PickNews news;

  /** 종목 선정 버킷 (이유 분류) */
  public enum PickBucket {
    STABLE, // 초보자 추천 (안정)
    REPRESENTATIVE, // 섹터 대표
    MOMENTUM, // 모멘텀/수급
    VALUE, // 저평가
    NEWS // 이슈 종목
  }

  /** 관련 뉴스 정보 */
  @Getter
  @Builder
  public static class PickNews {
    private String title;
    private String url;
    private String publisher;
    private String publishedAt;
  }
}
